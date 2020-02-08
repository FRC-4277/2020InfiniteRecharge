/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.subsystems;

import java.util.Map;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class VisionSystem extends SubsystemBase {
  private ShuffleboardTab driverTab;
  
  /**
   * Creates a new VisionSystem.
   * 
   */
  public VisionSystem(ShuffleboardTab tab) {
    tab.add("PSEye")
    .withWidget(BuiltInWidgets.kCameraStream)
    .withPosition(3, 0)
    .withSize(3, 3)
    .withProperties(
      Map.of("Show crosshair", true, "Show controls", true)
    );
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
