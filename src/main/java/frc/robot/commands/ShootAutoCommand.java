/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.VerticalHopper;

// NOTE:  Consider using this command inline, rather than writing a subclass.  For more
// information, see:
// https://docs.wpilib.org/en/latest/docs/software/commandbased/convenience-features.html
public class ShootAutoCommand extends SequentialCommandGroup {
  /**
   * Creates a new ShootAutoCommand.
   */
  public ShootAutoCommand(VerticalHopper hopper, Shooter shooter) {
    // Add your commands in the super() call, e.g.
    // super(new FooCommand(), new BarCommand());
    super(new ParallelCommandGroup(new ShooterForwardCommand(shooter), 
    new SequentialCommandGroup(
      new WaitCommand(1.0),
      new MoveHopperUpCommand(hopper),
      new WaitCommand(0.4),
      new StopHopperCommand(hopper),
      new WaitCommand(0.4),
      new MoveHopperUpCommand(hopper),
      new WaitCommand(0.4),
      new StopHopperCommand(hopper),
      new WaitCommand(0.4),
      new MoveHopperUpCommand(hopper),
      new WaitCommand(0.4),
      new StopHopperCommand(hopper),
      new WaitCommand(0.4),
      new MoveHopperUpCommand(hopper),
      new WaitCommand(0.4),
      new StopHopperCommand(hopper),
      new WaitCommand(0.4),
      new StopShooterCommand(shooter)
    )));
  }
}
