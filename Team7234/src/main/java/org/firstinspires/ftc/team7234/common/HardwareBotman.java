package org.firstinspires.ftc.team7234.common;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.hardware.configuration.ExpansionHubMotorControllerPositionParams;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

/**
 * Team 7234
 * This is NOT an OpMode
 *
 * This class contains methods for initialization and control of the robot
 *@author Donald Brown
 */
public class HardwareBotman
{
    //region Public OpMode members

    public DcMotor leftFrontDrive   = null;
    public DcMotor rightFrontDrive = null;
    public DcMotor leftBackDrive = null;
    public DcMotor rightBackDrive = null;
    public DcMotor arm     = null;
    public DcMotor relicArm = null;
    public Servo   leftClaw    = null;
    public Servo   rightClaw   = null;
    public Servo   jewelPusher = null;
    public Servo   relicClaw = null;

    public DigitalChannel armLimit = null;

    public ColorSensor jewelColorSensor = null;

    public BNO055IMU imu;
    public Orientation angles;
    //endregion


    //region Values
    public float hsvValues[] = {0F, 0F, 0F};

    public static final double MID_SERVO       =  0.5 ;

    public static final double RIGHT_GRIPPER_OPEN    =  0 ;
    public static final double LEFT_GRIPPER_OPEN  = 1 ;

    public static final double RIGHT_GRIPPER_HALF = 0.7;
    public static final double LEFT_GRIPPER_HALF = 0.3;

    public static final double RIGHT_GRIPPER_CLOSED    =  1 ;
    public static final double LEFT_GRIPPER_CLOSED  = 0;

    public static final double JEWEL_PUSHER_UP = 0.32;
    public static final double JEWEL_PUSHER_DOWN = 1;

    private static final double RELIC_ARM_BOTTOM = 0.0;
    private static final double RELIC_ARM_TOP = 1.0;

    //endregion

    //Establishes variables for motors
    public double[] mecanumSpeeds = {0.0, 0.0, 0.0, 0.0};
    public DcMotor[] driveMotors;

    public enum GripperState{
        OPEN,
        HALFWAY,
        CLOSED;

        private static GripperState[] vals = values();

        public GripperState next(){  //Code from https://stackoverflow.com/questions/17006239/whats-the-best-way-to-implement-next-and-previous-on-an-enum-type
            return vals[(this.ordinal()+1) % vals.length];
        }
        public GripperState previous(){
            int n = (this.ordinal()-1 % vals.length < 0) ? vals.length-1 : this.ordinal()-1 % vals.length;
            return vals[n];
        }
    }

    /* local OpMode members. */
    private HardwareMap hwMap           =  null;
    private ElapsedTime period  = new ElapsedTime();

    /* Constructor */
    public HardwareBotman(){}

    //alternative init to accomodate older code without Zero Power Behavior
    public void init(HardwareMap ahwMap, boolean reverseRight, DcMotor.ZeroPowerBehavior behavior){
        init(ahwMap, reverseRight);
        setMotorFloatMode(behavior);
    }
    /* Initialize standard Hardware interfaces */
    public void init(HardwareMap ahwMap, boolean reverseRight) {
        // Save reference to Hardware map
        hwMap = ahwMap;

        // Define and Initialize Motors
        leftFrontDrive  = hwMap.get(DcMotor.class, "left Front Drive");
        rightFrontDrive = hwMap.get(DcMotor.class, "right Front Drive");
        leftBackDrive = hwMap.get(DcMotor.class, "left Back Drive");
        rightBackDrive = hwMap.get(DcMotor.class, "right Back Drive");
        arm    = hwMap.get(DcMotor.class, "arm");
        relicArm = hwMap.get(DcMotor.class, "relicArm");


        if (reverseRight){
            leftFrontDrive.setDirection(DcMotor.Direction.FORWARD); // Set to REVERSE if using AndyMark motors
            leftBackDrive.setDirection(DcMotor.Direction.FORWARD);
            rightFrontDrive.setDirection(DcMotor.Direction.REVERSE);// Set to TWIST_CW if using AndyMark motors
            rightBackDrive.setDirection(DcMotor.Direction.REVERSE);
        }
        else{
            leftFrontDrive.setDirection(DcMotor.Direction.FORWARD); // Set to REVERSE if using AndyMark motors
            leftBackDrive.setDirection(DcMotor.Direction.FORWARD);
            rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);// Set to TWIST_CW if using AndyMark motors
            rightBackDrive.setDirection(DcMotor.Direction.FORWARD);
        }


        // Set all motors to zero power
        leftFrontDrive.setPower(0);
        rightFrontDrive.setPower(0);
        leftBackDrive.setPower(0);
        rightBackDrive.setPower(0);
        arm.setPower(0);

        // Resets the encoders of the robot
        resetEncoders();

        // Define and initialize ALL installed servos.
        leftClaw  = hwMap.get(Servo.class, "leftClaw");
        rightClaw = hwMap.get(Servo.class, "rightClaw");
        jewelPusher = hwMap.get(Servo.class, "jewelPusher");
        relicClaw = hwMap.get(Servo.class, "relicClaw");

        leftClaw.setPosition(LEFT_GRIPPER_OPEN);
        rightClaw.setPosition(RIGHT_GRIPPER_OPEN);
        jewelPusher.setPosition(JEWEL_PUSHER_UP);
        relicClaw.scaleRange(RELIC_ARM_BOTTOM, RELIC_ARM_TOP);

        //Define sensors
        jewelColorSensor = hwMap.get(ColorSensor.class, "jewelColorSensor");
        armLimit = hwMap.get(DigitalChannel.class, "armLimiter");

        driveMotors  = new DcMotor[] {leftFrontDrive, rightFrontDrive, leftBackDrive, rightBackDrive};

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode

        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        imu = hwMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);
    }

    public void setMotorFloatMode(DcMotor.ZeroPowerBehavior behavior){
        for (int i=0; i < 4; i++){
            driveMotors[i].setZeroPowerBehavior(behavior);
        }
    }

    public double heading(){
        angles   = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        return angles.firstAngle;
    }


    //region Gripper Control

    public void gripperSet(GripperState state){
        switch (state){
            case OPEN:
                leftClaw.setPosition(LEFT_GRIPPER_OPEN);
                rightClaw.setPosition(RIGHT_GRIPPER_OPEN);
                break;
            case HALFWAY:
                leftClaw.setPosition(LEFT_GRIPPER_HALF);
                rightClaw.setPosition(RIGHT_GRIPPER_HALF);
                break;
            case CLOSED:
                leftClaw.setPosition(LEFT_GRIPPER_CLOSED);
                rightClaw.setPosition(RIGHT_GRIPPER_CLOSED);
                break;
            default: throw new IllegalArgumentException("GripperState contains an unexpected value. I'm not even sure how you managed this.");
        }
    }
    //endregion

    //region Robot Driving
    public void arrayDrive(double lf, double rf, double lb, double rb){
        leftFrontDrive.setPower(lf);
        rightFrontDrive.setPower(rf);
        leftBackDrive.setPower(lb);
        rightBackDrive.setPower(rb);
    }


    public void mecanumDrive(double angle, double magnitude, double rotation){  //Calculates and sends values to wheels
        //region Exceptions
        /*if(angle> 1.5 *Math.PI || angle< -0.5*Math.PI){
            throw new IllegalArgumentException("Angle is outside range [-pi/2, 3pi/2]. Invalid Value is: " + Double.toString(angle));
        }*/ //Realized exception is unecessary, as angle is never called outside of trigonometric functions

        if(magnitude<0 || magnitude>1){
            throw new IllegalArgumentException("Magnitude is outside range [0, 1]. Invalid Value is: " + Double.toString(magnitude));
        }
        if(rotation<-1 || rotation>1){
            throw new IllegalArgumentException("Rotation is outside range [-1, 1]. Invalid Value is: " + Double.toString(rotation));
        }
        //endregion
        //region Initial Speeds
        mecanumSpeeds[0] = ((magnitude*(Math.sin(angle+(Math.PI/4))))+rotation);
        mecanumSpeeds[1] = -((magnitude*(Math.cos(angle+(Math.PI/4))))-rotation);  //Generates Raw Values for Motors
        mecanumSpeeds[2] = ((magnitude*(Math.cos(angle+(Math.PI/4))))+rotation);
        mecanumSpeeds[3] = -((magnitude*(Math.sin(angle+(Math.PI/4))))-rotation);
        //endregion
        //region Speed Divider
        double speedDivider = Math.abs(mecanumSpeeds[0]);
        //Speed divider is set as
        for (int i=0; i<4; i++){
            if (Math.abs(mecanumSpeeds[i]) > speedDivider){
                speedDivider = Math.abs(mecanumSpeeds[i]);
            }
        }
        if (speedDivider > 1) {            //SpeedDivider is only called if it is necessary to maintain ranges
            for (int i=0; i<4; i++) {
                mecanumSpeeds[i] /= speedDivider;
            }
        }
        //endregion
        //region Power Assignment
        for (int i =0; i<4; i++){
            mecanumSpeeds[i] = Range.clip(mecanumSpeeds[i], -1.0, 1.0);
            driveMotors[i].setPower(mecanumSpeeds[i]);
        }
        //endregion
    }

    public double[] mecanumDeltas(double x, double y){
        return new double[]{
                ticsPerInch(x)*Math.sqrt(2.0)+ticsPerInch(y)*Math.sqrt(2.0), //Left Front
                ticsPerInch(x)*Math.sqrt(2.0)-ticsPerInch(y)*Math.sqrt(2.0), //Right Front
                -ticsPerInch(x)*Math.sqrt(2.0)+ticsPerInch(y)*Math.sqrt(2.0), //Left Back
                -ticsPerInch(x)*Math.sqrt(2.0)-ticsPerInch(y)*Math.sqrt(2.0) //Right Back
        };
    }
    //endregion

    //Function to limit values to a range
    private double clip(double input, double min, double max){   //Method for clipping a value within a range
        double output = input;
        if (input < min){
            output = min;
        }
        else if (input > max){
            output = max;
        }
        return output;
    }

    public void driveByGyro(double speed, double header){
        if(speed > 0.9 || speed < -0.9) {
            throw new IllegalArgumentException("Nah fam, keep it between -0.9 and 0.9" + speed);
        }
        if (heading() > header + 3) {
            arrayDrive(speed + 0.1, speed - 0.1, speed + 0.1, speed - 0.1);
        }
        else if (heading() < header - 3) {
            arrayDrive(speed - 0.1, speed + 0.1, speed - 0.1, speed + 0.1);
        }
        else{
            arrayDrive(speed, speed, speed, speed);
        }

    }

    public double ticsPerInch(double distance){
        return (560.0*distance/(4.0*Math.PI));
    }

    public void resetEncoders() {
        leftBackDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftFrontDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightBackDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightFrontDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftFrontDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightFrontDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        leftBackDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightBackDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

 }
