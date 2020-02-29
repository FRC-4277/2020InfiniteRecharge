/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.controller.RamseteController;
import edu.wpi.first.wpilibj.controller.SimpleMotorFeedforward;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryUtil;
import edu.wpi.first.wpilibj2.command.RamseteCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import static frc.robot.Constants.DriveTrain.*;


public class DriveTrain extends SubsystemBase {
  private WPI_TalonSRX frontLeftMotor = new WPI_TalonSRX(FRONT_LEFT);
  private WPI_TalonSRX frontRightMotor = new WPI_TalonSRX(FRONT_RIGHT);
  private WPI_TalonSRX backLeftMotor = new WPI_TalonSRX(BACK_LEFT);
  private WPI_TalonSRX backRightMotor = new WPI_TalonSRX(BACK_RIGHT);
  
  private SpeedControllerGroup leftGroup = new SpeedControllerGroup(frontLeftMotor, backLeftMotor);
  private SpeedControllerGroup rightGroup = new SpeedControllerGroup(frontRightMotor, backRightMotor);
  
  private AHRS navX = new AHRS(); //todo: add ADXRS450 as backup (remember proper math on #getAngle when using it)

  private DifferentialDrive drive = new DifferentialDrive(leftGroup, rightGroup);
  private DifferentialDriveOdometry odometry;
  private SimpleMotorFeedforward motorFeedforward
          = new SimpleMotorFeedforward(KS_VOLTS, KS_VOLT_SECONDS_PER_METER, KS_VOLT_SECONDS_SQUARED_PER_METER);

  /**
   * Creates a new DriveTrain.
   */
  public DriveTrain() {

    // Reset TalonSRX config
    frontLeftMotor.configFactoryDefault();
    frontRightMotor.configFactoryDefault();
    backLeftMotor.configFactoryDefault();
    backRightMotor.configFactoryDefault();

    // Make fronts follow backs as backs have encoders
    frontLeftMotor.follow(backLeftMotor);
    frontRightMotor.follow(backRightMotor);

    // Invert left side, so green is forward for it
    frontRightMotor.setInverted(true);
    backRightMotor.setInverted(true);

    // Don't let WPI invert, we already did through TalonSRX APIs
    drive.setRightSideInverted(false);

    // DriveTrain Velocity PID
    configureEncoderTalon(backLeftMotor);
    configureEncoderTalon(backRightMotor);
    backLeftMotor.setSensorPhase(LEFT_BACK_SENSOR_PHASE);
    backRightMotor.setSensorPhase(RIGHT_BACK_SENSOR_PHASE);

    /*SmartDashboard.putData(frontLeftMotor);
    SmartDashboard.putData(frontRightMotor);
    SmartDashboard.putData(backLeftMotor);
    SmartDashboard.putData(backRightMotor);*/

    resetEncoders();
    odometry = new DifferentialDriveOdometry(Rotation2d.fromDegrees(getHeading()));
  }

  private void configureEncoderTalon(TalonSRX talonSRX) {
    talonSRX.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative);
    talonSRX.configAllowableClosedloopError(0, DRIVE_VELOCITY_ERROR_TOLERANCE, 0);
    talonSRX.config_kP(0, DRIVE_P);
    talonSRX.config_kI(0, DRIVE_I);
    talonSRX.config_kD(0, DRIVE_D);
    talonSRX.configVelocityMeasurementWindow(ROLLING_VELOCITY_SAMPLES);
    talonSRX.configVelocityMeasurementPeriod(VELOCITY_MEAS_PERIOD);
    talonSRX.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, STATUS_2_FEEDBACK_MS);
    talonSRX.setStatusFramePeriod(StatusFrameEnhanced.Status_3_Quadrature, STATUS_3_QUADRATURE_MS);
  }

  public void resetEncoders() {
    backLeftMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative);
    backRightMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative);
    backLeftMotor.setSelectedSensorPosition(0);
    backRightMotor.setSelectedSensorPosition(0);
  }

  public Pose2d getPose() {
    return odometry.getPoseMeters();
  }

  public double ticksToRotations(int ticks) {
    return ticks / (double) ENCODER_TICKS_PER_ROTATION;
  }

  public double rotationsToTicks(double rotations) {
    return rotations * ENCODER_TICKS_PER_ROTATION;
  }

  public double ticksToMeters(int ticks) {
    return ticksToRotations(ticks) * WHEEL_CIRCUMFERENCE_METERS;
  }

  public double metersToTicks(double meters) {
    double rotations = meters / WHEEL_CIRCUMFERENCE_METERS;
    return rotationsToTicks(rotations);
  }

  public DifferentialDriveWheelSpeeds getWheelSpeeds() {
    int leftTicksPerDs = backLeftMotor.getSelectedSensorVelocity();
    double leftMetersPerSecond = ticksToMeters(leftTicksPerDs * 10);

    int rightTicksPerDs = backRightMotor.getSelectedSensorVelocity();
    double rightMetersPerSecond = ticksToMeters(rightTicksPerDs * 10);

    return new DifferentialDriveWheelSpeeds(leftMetersPerSecond, rightMetersPerSecond);
  }

  public void resetOdometry(Pose2d pose) {
    resetEncoders();
    odometry.resetPosition(pose, Rotation2d.fromDegrees(getHeading()));
  }

  public double getLeftDistanceM() {
    int leftTicks = backLeftMotor.getSelectedSensorPosition();
    return ticksToMeters(leftTicks);
  }

  public double getRightDistanceM() {
    int rightTicks = backRightMotor.getSelectedSensorPosition();
    return ticksToMeters(rightTicks);
  }


  public double getAverageEncoderDistanceM() {
    return (getLeftDistanceM() + getRightDistanceM()) / 2.0; // average of both sides
  }

  public void zeroHeading() {
    navX.reset();
  }

  /**
   *
   * @return -180..180
   */
  public double getHeading() {
    return navX.getYaw();
  }

  public double getTurnRate() {
    return navX.getRate();
  }

  @Override
  public void periodic() {
    odometry.update(Rotation2d.fromDegrees(getHeading()), getLeftDistanceM(), getRightDistanceM());
  }
  
  public void joystickDrive(double forwardSpeed, double rotation, boolean quick) {
    //drive.arcadeDrive(x, y);
    if (forwardSpeed <= .15) {
      drive.curvatureDrive(forwardSpeed, rotation, true);
    } else {
      drive.curvatureDrive(forwardSpeed, rotation, quick);
    }
  }

  public void rawTankDrive(double leftSpeed, double rightSpeed) {
    drive.tankDrive(leftSpeed, rightSpeed, false);
  }

  public Trajectory generateTrajectory(String pathFileName) {
    String path = "paths/" + pathFileName + ".wpilib.json";
    try {
      Path trajectoryPath = Filesystem.getDeployDirectory().toPath().resolve(path);
      return TrajectoryUtil.fromPathweaverJson(trajectoryPath);
    } catch (IOException e) {
      DriverStation.reportError("Unable to open trajectory: " + path, e.getStackTrace());
    }
    return null;
  }

  public RamseteCommand generateRamseteCommand(Trajectory trajectory) {
    return new RamseteCommand(
            trajectory,
            this::getPose,
            new RamseteController(kRamseteB, kRamseteZeta),
            KINEMATICS,
            new BiConsumer<>() {
              private double prevLeftMPS, prevRightMPS;
              private double prevTime;
              private boolean firstRun = true;
              private Timer timer = new Timer();

              @Override
              public void accept(Double leftMetersPerSecond, Double rightMetersPerSecond) {
                double currentTime = timer.get();
                if (firstRun) {
                  DifferentialDriveWheelSpeeds wheelSpeeds = getWheelSpeeds();
                  prevLeftMPS = wheelSpeeds.leftMetersPerSecond;
                  prevRightMPS = wheelSpeeds.rightMetersPerSecond;

                  timer.reset();
                  timer.start();

                  prevTime = currentTime;
                  firstRun = false;
                }


                double dt = currentTime - prevTime;
                boolean dtIsPositive = dt > 0;

                double leftAcceleration = dtIsPositive ? (leftMetersPerSecond - prevLeftMPS) / dt : 0;
                double leftFeedforwardVolts = motorFeedforward.calculate(leftMetersPerSecond, leftAcceleration);
                double leftFeedforward = leftFeedforwardVolts / MAX_BATTERY_V;
                double leftTicksPerSecond = metersToTicks(leftMetersPerSecond);
                double leftTicksPerDs = leftTicksPerSecond / 10;
                backLeftMotor.set(ControlMode.Velocity, leftTicksPerDs, DemandType.ArbitraryFeedForward, leftFeedforward);

                double rightAcceleration = dtIsPositive ? (rightMetersPerSecond - prevRightMPS) / dt : 0;
                double rightFeedforwardVolts = motorFeedforward.calculate(rightMetersPerSecond, rightAcceleration);
                double rightFeedforward = rightFeedforwardVolts / MAX_BATTERY_V;
                double rightTicksPerSecond = metersToTicks(rightMetersPerSecond);
                double rightTicksPerDs = rightTicksPerSecond / 10;
                backRightMotor.set(ControlMode.Velocity, rightTicksPerDs, DemandType.ArbitraryFeedForward, rightFeedforward);

                System.out.println("SK: " + leftMetersPerSecond + " R:" + rightMetersPerSecond);

                prevLeftMPS = leftMetersPerSecond;
                prevRightMPS = rightMetersPerSecond;
                prevTime = currentTime;

                drive.feed(); //So watchdog won't kill it
              }
            },
            this
    );
  }
}
