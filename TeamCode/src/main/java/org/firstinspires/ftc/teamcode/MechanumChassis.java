package org.firstinspires.ftc.teamcode;

import android.graphics.Color;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.blockplacer.BlockColors;
import org.firstinspires.ftc.teamcode.blockplacer.CryptoboxColumns;

/**
 * Created by matt on 9/16/17.
 */

public class MechanumChassis {

    // Output members, corresponds to motors.
    /*
         Front
        ________
        |0    1|
        |      |
        |3    2|
        --------
         Rear
    */

    private HardwareMap hardwareMap;

    /**
     * Private hardware... Should not need to be accessed outside
     * of this class for ANY REASON.
     */

    // Drive train motor speeds, hardware, and motion vector
    private double speed0;
    private double speed1;
    private double speed2;
    private double speed3;
    private DcMotor m0;
    private DcMotor m1;
    private DcMotor m2;
    private DcMotor m3;
    private Vector2D moveVec = new Vector2D(1, 0);


    // IMU
    private BNO055IMU imu;

    /**
     * Public hardware... May need to be accessed from outside this
     * class, which is perfectly acceptable.
     */

    // Scoring mechanism hardware
    DcMotor lift;
    DcMotor relic;
    DcMotor lCollect;
    DcMotor rCollect;
    Servo lCollectServo;
    Servo rCollectServo;
    DistanceSensor upperBlock;
    ColorSensor upperBlockCS;
    DistanceSensor lowerBlock;
    ColorSensor lowerBlockCS;
    DistanceSensor intakeBlock;
    Servo flipperLeft;
    Servo flipperRight;

    // Jewel kicking hardware
    Servo rTentacle;
    Servo lTentacle;
    ColorSensor jsL;
    ColorSensor jsR;
    ColorSensor js;
    DistanceSensor jsLD;
    DistanceSensor jsRD;
    DistanceSensor jsD;

    // Relic hardware
    Servo armServo;
    Servo clawServo;

    // Autonomous alignment tool hardware
    Servo lowerAlign;
    Servo topAlign;
    DigitalChannel sideSwitch;
    DigitalChannel lowerFrontSwitch;
    DigitalChannel upperFrontSwitch;

    int lowerBlockColor = -1;
    int upperBlockColor = -1;

    // Useful constant
    public final int SERVO_DEPLOYMENT_TIME = 500;


    private float imuInitOffset = -9000;
    private float teleopHeading = -9000;

    private float tweenTime = 700;
    private float rotationTarget = 0;
    public boolean debugModeEnabled = false;
    private LinearOpMode context;

    boolean justPark = false;

    // Not setting to 1 allows for headroom for motors to reach requested velocities.
    public final float powerConstant = 0.9f;

    // Teleop style instance
    MechanumChassis(HardwareMap h) {
        this.hardwareMap = h;
    }

    // Autonomous style instance
    MechanumChassis(HardwareMap h, LinearOpMode c) {
        this.hardwareMap = h;
        this.context = c;
    }

    void initializeWithIMU(){
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);
        initialize();
    };

    void initialize() {
        // TODO: Add hardware validator

        setDirectionVector(moveVec);
        initMotors();

        lCollectServo = hardwareMap.servo.get("lFlip");
        rCollectServo = hardwareMap.servo.get("rFlip");
        raiseIntake();

        relic = hardwareMap.dcMotor.get("relic");
        relic.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        relic.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        lowerAlign = hardwareMap.servo.get("rAlignServo");
        lowerAlign.setPosition(ServoValue.LOWER_ALIGN_IN);
        topAlign = hardwareMap.servo.get("topAlign");
        topAlign.setPosition(ServoValue.TOP_ALIGN_IN);

        armServo = hardwareMap.servo.get("armServo");
        armServo.setPosition(ServoValue.RELIC_ARM_STORAGE);
        clawServo = hardwareMap.servo.get("clawServo");
        clawServo.setPosition(ServoValue.RELIC_CLAW_GRAB);

        lift = hardwareMap.dcMotor.get("lBelting");
        lift.setDirection(DcMotor.Direction.REVERSE);
        lCollect = hardwareMap.dcMotor.get("lCollect");
        rCollect = hardwareMap.dcMotor.get("rCollect");
        rCollect.setDirection(DcMotor.Direction.REVERSE);
        flipperLeft = hardwareMap.servo.get("flipperLeft");
        flipperRight = hardwareMap.servo.get("flipperRight");
        stowFlipper();

        rTentacle = hardwareMap.servo.get("rTentacle");
        lTentacle = hardwareMap.servo.get("lTentacle");
        lTentacle.setPosition(ServoValue.LEFT_TENTACLE_UP);
        rTentacle.setPosition(ServoValue.RIGHT_TENTACLE_UP);

        upperBlock = hardwareMap.get(DistanceSensor.class, "upperBlock");
        upperBlockCS = hardwareMap.get(ColorSensor.class, "upperBlock");

        lowerBlock = hardwareMap.get(DistanceSensor.class, "lowerBlock");
        lowerBlockCS = hardwareMap.get(ColorSensor.class, "lowerBlock");

        intakeBlock = hardwareMap.get(DistanceSensor.class, "intakeBlock");

        sideSwitch = hardwareMap.get(DigitalChannel.class, "upperSide");
        upperFrontSwitch = hardwareMap.get(DigitalChannel.class, "upperFront");
        lowerFrontSwitch = hardwareMap.get(DigitalChannel.class, "frontSwitch");
    }

    private void initMotors() {
        m0 = hardwareMap.dcMotor.get("m0");
        m1 = hardwareMap.dcMotor.get("m1");
        m2 = hardwareMap.dcMotor.get("m2");
        m3 = hardwareMap.dcMotor.get("m3");
        m0.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        m1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        m2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        m3.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        m3.setDirection(DcMotor.Direction.REVERSE);
        m0.setDirection(DcMotor.Direction.REVERSE);
    }

    private void stopMotors() {
        m0.setPower(0);
        m1.setPower(0);
        m2.setPower(0);
        m3.setPower(0);
    }

    //based on second post here ftcforum.usfirst.org/forum/ftc-technology/android-studio/6361-mecanum-wheels-drive-code-example
    void setDirectionVector(Vector2D vector) {
        double magnitude = Math.hypot(vector.GetXComponent(), vector.GetYComponent());
        double robotAngle = Math.atan2(vector.GetYComponent(), vector.GetXComponent()) - (Math.PI / 4);
        speed0 = magnitude * Math.cos(robotAngle) * powerConstant;
        speed1 = magnitude * Math.sin(robotAngle) * powerConstant;
        speed2 = magnitude * Math.cos(robotAngle) * powerConstant;
        speed3 = magnitude * Math.sin(robotAngle) * powerConstant;
    }

    // Helper method to avoid editing and setting vector everytime movment direction changes.
    void setDirectionVectorComponents(double x, double y) {
        moveVec.SetComponents(x, y);
        setDirectionVector(moveVec);
    }

    void setTweenTime(float millis) {
        this.tweenTime = millis;
    }

    void addTeleopIMUTarget(double joyInput, Telemetry tm) {
        teleopHeading += 4 * joyInput;
        tm.addData("(rotation, teleopHeading, P value)", getRotation() + ", " + (getRotation() - teleopHeading) + ", " + teleopHeading);
        tm.update();
        setMotorPowers(1, getOffset(getRotation(), teleopHeading) / 40);
    }

    void lockRotation() {
        if(teleopHeading == -9000 /* has not been set already */) {
            teleopHeading = getRotation();
        }
        setMotorPowers(1, getOffset(getRotation(), teleopHeading) / 27);
    }

    private float getRotation() {
        return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle + 180;
    }

    public void setRotationTarget(float degrees) {
        if(imuInitOffset == -9000 /* has not been set already */) {
            imuInitOffset = getRotation();
        }

        rotationTarget = imuInitOffset - degrees;
    }

    public void turnToTarget() {
        turnToTarget(false);
    }

    public void turnToTarget(boolean shouldCollect) {
        long start = System.currentTimeMillis();
        long turnTimeout = 3000;
        float P;
        while(context.opModeIsActive() && start + turnTimeout > System.currentTimeMillis()) {

            if(shouldCollect) {
                safeIntakeAsync();
            } else {
                lift.setPower(0);
            }

            P = getOffset(getRotation(), rotationTarget);

            if(Math.abs(P) < 0.07) {
                break;
            }

            if (debugModeEnabled) {
                context.telemetry.addData("turnToTarget", "target: "+ rotationTarget);
                context.telemetry.addData("turnToTarget", "rotation: "+ getRotation());
                context.telemetry.addData("turnToTarget", "Rotational Error: " + P);
                context.telemetry.update();
            }

            m0.setPower(P / 40);
            m1.setPower(-P / 40);
            m2.setPower(-P / 40);
            m3.setPower(P / 40);
        }
        stopMotors();
    }

    private float getOffset(float currentAngle, float target) {
        if (currentAngle + 360 - target <= 180) {
            return (currentAngle -  target + 360);
        } else if (target + 360 - currentAngle <= 180) {
            return (target - currentAngle + 360) * -1;
        } else if (currentAngle -  target <= 180) {
            return (currentAngle -  target);
        }
        return 0;
    }

    /***
     * This method is separated from a normal turn because we don't care about precision but more importantly,
     * the oscillations during a P turn will cause the robot to rock off the balance board at a skewed angle, thus loosing traction.
     * A single imprecise turn fixes this issue.
     */
    public void jewelKick(int direction) {
        // Tentacles are deployed elsewhere for color detection in main OpMode.
        encoderTurn(-direction, 700);
        stowTentacles();
        encoderTurn(direction, 700);
    }

    public void encoderTurn(int direction, long turnTime) {
        long start = System.currentTimeMillis();
        while (start + turnTime > System.currentTimeMillis() && context.opModeIsActive()) {
            m0.setPower(-0.125*direction);
            m1.setPower(0.125*direction);
            m2.setPower(0.125*direction);
            m3.setPower(-0.125*direction);
        }
        stopMotors();
    }


    private TesseractTimer intakeStallTimer = new TesseractTimer(1);
    private TesseractTimer shiftPastSensorTime = new TesseractTimer(1);
    boolean collectionFinished = false;
    private TesseractTimer glyphRotateTimeout = new TesseractTimer(1);
    // Todo: Maybe reimplement canLift safety, probably unnecessary though.
    void safeIntakeAsync() {
        if(upperBlock != null && lowerBlock != null && canLift()) {
            if (!hasIntakeBlock()) {
                context.telemetry.addData("Intake State: ", "No Block");
                lCollect.setPower(.8);
                rCollect.setPower(.8);
                if(shiftPastSensorTime.hasReachedTimeout()) {
                    lift.setPower(0);
                } else {
                    lift.setPower(1);
                }
                intakeStallTimer = new TesseractTimer(4000);
            } else if (hasIntakeBlock()) {
                shiftPastSensorTime = new TesseractTimer(1000);
                if (intakeStallTimer.hasReachedTimeout()) {
                    glyphRotateTimeout = new TesseractTimer(1000);
                    intakeStallTimer = new TesseractTimer(4000);
                } else if (!glyphRotateTimeout.hasReachedTimeout()) {
                    lCollect.setPower(-.8);
                    rCollect.setPower(.8);
                    lift.setPower(0);
                } else {
                    context.telemetry.addData("Intake State: ", "Has Block");
                    lift.setPower(1);
                    lCollect.setPower(.8);
                    rCollect.setPower(.8);
                }
            }

            if(hasLowerBlock() && hasIntakeBlock()) {
                collectionFinished = true;
            }

            // Continue running until canLift() returns false (in the above while loop) after both blocks are collected.
            if(collectionFinished) {
                lift.setPower(1);
            }
        } else {
            lift.setPower(0);
            lCollect.setPower(0);
            rCollect.setPower(0);
            collectionFinished = true;
        }
        context.telemetry.update();
    }

    // Returns truthy if no block is at the very top of the scoring mechanism.
    boolean canLift() {
        return upperBlock.getDistance(DistanceUnit.CM) > 15 || Double.isNaN(upperBlock.getDistance(DistanceUnit.CM));
    }

    boolean hasLowerBlock() {
        return !Double.isNaN(lowerBlock.getDistance(DistanceUnit.CM)) && !(lowerBlock.getDistance(DistanceUnit.CM) > 10);
    }

    // Returns truthy if there is a block in the intake.
    boolean hasIntakeBlock() {
        return !Double.isNaN(intakeBlock.getDistance(DistanceUnit.CM)) && !(intakeBlock.getDistance(DistanceUnit.CM) > 50);
    }

    // This method acts as cleanup for safeIntakeAsync. Basically if blocks are still not correctly staged in the scoring mech by the end
    // of whatever motion safeIntakeAsync was being used in, then intakeUntilStaged will finish the job.
    void intakeUntilStaged() {
        TesseractTimer t = new TesseractTimer(5000);
        // This will clear the lowest block into from the intake and into the scoring mech.
        while(canLift() && hasIntakeBlock() && context.opModeIsActive() && !t.hasReachedTimeout()) {
            lift.setPower(1);
            lCollect.setPower(.8);
            rCollect.setPower(.8);
        }
        while(!hasLowerBlock() && context.opModeIsActive() && !t.hasReachedTimeout()) {
            lift.setPower(-1);
        }
        // There is now guaranteed a block at lower block pos, the one we just cleared from the intake.
        if(lowerBlockColor == -1) lowerBlockColor = getCurrentBlockColor(lowerBlockCS);
        // Now move the upper block to a position where the top sensor can read it.
        while (canLift() && context.opModeIsActive() && !t.hasReachedTimeout()) {
            lift.setPower(1);
            lCollect.setPower(.8);
            rCollect.setPower(.8);
        }

        lift.setPower(0);
        lCollect.setPower(0);
        rCollect.setPower(0);

        upperBlockColor = getCurrentBlockColor(upperBlockCS);
        context.telemetry.addData("upperBlockColor", upperBlockColor);
        context.telemetry.update();
        if(t.hasReachedTimeout()) {
            justPark = true;
        }
    }

    void run(long m, float s, float e) {
        run(m, s, e, false);
    }

    void run(long millis, float startSpeed, float endSpeed, boolean runBelting) {
        long start = System.currentTimeMillis();
        float P;
        float elapsedTime;
        while (start + millis > System.currentTimeMillis() && context.opModeIsActive()) {
            if(runBelting) {
                if(hasIntakeBlock()) {
                    break;
                } else {
                    rCollect.setPower(1);
                }
            }
            elapsedTime = System.currentTimeMillis() - start;
            P = getOffset(getRotation(), rotationTarget) / 15;
            setMotorPowers(calculateTweenCurve(millis, elapsedTime, startSpeed, endSpeed), P);
        }
        stopMotors();
        if(runBelting) {
            clearIntake();
        }
    }

    // Curviness is in periods per second
    float runAlongCurve(long millis, long startTime, float speed, float turnAmplitude, int curviness, boolean runBelting, int sideModifier, int startOffset) {
        long start = System.currentTimeMillis() - startTime;
        float P;
        float elapsedTime = System.currentTimeMillis() - start;
        float endTime = 0;
        TesseractTimer goForABitLonger = new TesseractTimer(30000);

        boolean intakeWasTriggered = false;
        boolean nextBlock = false;

        while (start + millis > System.currentTimeMillis() && context.opModeIsActive()) {

            if(!nextBlock) {
                if (!intakeWasTriggered) {
                    if (!hasIntakeBlock()) {
                        rCollect.setPower(1);
                        if(startOffset == 0) {
                            lCollect.setPower(1);
                        }
                    } else {
                        intakeWasTriggered = true;
                    }
                } else {
                    if (!hasLowerBlock()) {
                        rCollect.setPower(1);
                        lCollect.setPower(1);
                        lift.setPower(1);
                    } else {
                        rCollect.setPower(0);
                        lCollect.setPower(0);
                        lift.setPower(0);
                        nextBlock = true;
                        goForABitLonger = new TesseractTimer(200);
                        intakeWasTriggered = false;
                    }
                }
            } else {
                if(!goForABitLonger.hasReachedTimeout()) {
                    rCollect.setPower(1);
                    lCollect.setPower(1);
                    lift.setPower(1);
                } else {
                    if (!intakeWasTriggered) {
                        if (!hasIntakeBlock()) {
                            rCollect.setPower(1);
                            if(startOffset == 0) {
                                lCollect.setPower(1);
                            } else {
                                lCollect.setPower(0);
                            }
                            lift.setPower(0);
                        } else {
                            intakeWasTriggered = true;
                        }
                    } else {
                        if (hasIntakeBlock() && canLift()) {
                            rCollect.setPower(1);
                            lCollect.setPower(1);
                            lift.setPower(1);
                        } else {
                            break;
                        }
                    }
                }
            }
            elapsedTime = System.currentTimeMillis() - start;

            setRotationTarget(
                    (float)
                    (turnAmplitude*Math.sin(((curviness*Math.PI) / 1000) * elapsedTime)) + (startOffset*sideModifier) // side modify
            );

            P = getOffset(getRotation(), rotationTarget) / 15;
            setMotorPowers(speed, P);
        }
        stopMotors();
        lift.setPower(0);
        rCollect.setPower(0);
        lCollect.setPower(0);
        return endTime;
    }

    void clearIntake() {
        TesseractTimer uptakeTimeout = new TesseractTimer(1000);
        while ((hasIntakeBlock() || !hasLowerBlock()) && !uptakeTimeout.hasReachedTimeout() && canLift() && context.opModeIsActive()) {
            lift.setPower(1);
            lCollect.setPower(1);
            rCollect.setPower(1);
        }
        TesseractTimer shiftFurtherTimeout = new TesseractTimer(400);
        while (!shiftFurtherTimeout.hasReachedTimeout() && context.opModeIsActive()) {
            if(canLift()) {
                lift.setPower(1);
                lCollect.setPower(1);
                rCollect.setPower(1);
            } else {
                lift.setPower(0);
                lCollect.setPower(0);
                rCollect.setPower(0);
            }
        }
        lift.setPower(0);
        lCollect.setPower(0);
        rCollect.setPower(0);
    }

    void stowAlignment() {
        lowerAlign.setPosition(ServoValue.LOWER_ALIGN_IN);
        topAlign.setPosition(ServoValue.TOP_ALIGN_IN);
    }

    void deployAlignment(int column) {
        topAlign.setPosition((column == CryptoboxColumns.RIGHT) ? ServoValue.TOP_ALIGN_OUT-0.14f : ServoValue.TOP_ALIGN_OUT);
        lowerAlign.setPosition((column == CryptoboxColumns.RIGHT) ? ServoValue.LOWER_ALIGN_OUT : ServoValue.LOWER_ALIGN_IN);
//        context.sleep(SERVO_DEPLOYMENT_TIME);
    }

    void stowTentacles() {
        lTentacle.setPosition(ServoValue.LEFT_TENTACLE_UP);
        rTentacle.setPosition(ServoValue.RIGHT_TENTACLE_UP + .1);
        context.sleep(SERVO_DEPLOYMENT_TIME);
    }

    void deployTentacles() {
        lTentacle.setPosition(ServoValue.LEFT_TENTACLE_DOWN);
        rTentacle.setPosition(ServoValue.RIGHT_TENTACLE_DOWN);
        context.sleep(SERVO_DEPLOYMENT_TIME);
    }

    void stowFlipper() {
        flipperRight.setPosition(ServoValue.FLIPPER_RIGHT_DOWN);
        flipperLeft.setPosition(ServoValue.FLIPPER_LEFT_DOWN);
    }

    void deployFlipper() {
        flipperRight.setPosition(ServoValue.FLIPPER_RIGHT_UP);
        flipperLeft.setPosition(ServoValue.FLIPPER_LEFT_UP);
    }

    int getCurrentBlockColor(ColorSensor cs) {
        context.telemetry.log().add("blockColorRatio", (float) cs.red() / (float) cs.blue());
        context.telemetry.update();
        if((float) cs.red() / (float) cs.blue() < 1.4) {
            return BlockColors.GREY;
        } else {
            return BlockColors.BROWN;
        }
    }

    void homeToCryptoColumn() {
        homeToCryptoColumn(30000);
    }

    void homeToCryptoColumn(long timeout) {
        float P;
        Vector2D movementDirection = new Vector2D(0, 1);
        setDirectionVector(movementDirection);

        TesseractTimer to = new TesseractTimer(timeout);

        // DigitalChannel.getState() is false when pressed.

        while((lowerFrontSwitch.getState() && upperFrontSwitch.getState()) && context.opModeIsActive() && !to.hasReachedTimeout()) {

            // If we came in pressed we want to undo that before going straight.
            if(!sideSwitch.getState()) {
                movementDirection = new Vector2D(1, 0);
                setDirectionVector(movementDirection);
            } else {
                movementDirection = new Vector2D(0, 1);
                setDirectionVector(movementDirection);
            }
            P = getOffset(getRotation(), rotationTarget) / 30;
            setMotorPowers(0.3f, P); //.2
        }

        movementDirection = new Vector2D(0, -1);
        setDirectionVector(movementDirection);
        float oldTween = this.tweenTime;
        tweenTime = 0;
        run(300, 0, 0.3f);
        tweenTime = oldTween;

        movementDirection = new Vector2D(-1, 0);
        setDirectionVector(movementDirection);

        long start = System.currentTimeMillis();
        long turnTimeout = 2000;

        while(sideSwitch.getState() && context.opModeIsActive() && start + turnTimeout > System.currentTimeMillis()) {
            P = getOffset(getRotation(), rotationTarget) / 20;
            setMotorPowers(0.2f, P);
        }

        stopMotors();
    }
    void runToFront() {
        float P;
        Vector2D movementDirection = new Vector2D(0, 1);
        setDirectionVector(movementDirection);

        // DigitalChannel.getState() is false when pressed.

        while((lowerFrontSwitch.getState() && upperFrontSwitch.getState()) && context.opModeIsActive()) {
            P = getOffset(getRotation(), rotationTarget) / 30;
            setMotorPowers(0.4f, P);
        }
        stopMotors();
    }

    void runToSide() {
        float P;
        Vector2D movementDirection = new Vector2D(-1, 0);
        setDirectionVector(movementDirection);

        // DigitalChannel.getState() is false when pressed.

        while(sideSwitch.getState() && context.opModeIsActive()) {
            P = getOffset(getRotation(), rotationTarget) / 30;
            setMotorPowers(0.4f, P);
        }
        stopMotors();
    }

    void raiseIntake() {
        lCollectServo.setPosition(ServoValue.LEFT_COLLECT_UP);
        rCollectServo.setPosition(ServoValue.RIGHT_COLLECT_UP);
    }

    void lowerIntake() {
        lCollectServo.setPosition(ServoValue.LEFT_COLLECT_DOWN);
        rCollectServo.setPosition(ServoValue.RIGHT_COLLECT_DOWN);
    }

    void addJoystickRotation(double rotation){
        speed0 += rotation;
        speed1 -= rotation;
        speed2 -= rotation;
        speed3 += rotation;
    }

    private double calculateTweenCurve(long millis, float elapsedTime, double startSpeed, double endSpeed) {
        double tmpReturn;
        if (elapsedTime <= tweenTime) {
            tmpReturn = ((startSpeed - endSpeed)/2) * Math.cos((Math.PI*elapsedTime) / tweenTime) + (startSpeed + endSpeed) / 2;
//            context.telemetry.log().add(String.valueOf(tmpReturn));
            return tmpReturn;
        } else if (elapsedTime < millis - tweenTime) {
            return endSpeed;
        } else { // elapsedTime > millis - tweenTime
            tmpReturn = ((endSpeed - startSpeed)/2) * Math.cos((Math.PI*(millis-tweenTime-elapsedTime)) / tweenTime) + (endSpeed + startSpeed) / 2;
            context.telemetry.log().add(String.valueOf(tmpReturn));
            context.telemetry.update();
            return tmpReturn;
        }
    }

    void setMotorPowers() {
        setMotorPowers(1, 0);
    }

    private void setMotorPowers(double power, float P) {
        m0.setPower(speed0 * power + P);
        m1.setPower(speed1 * power - P);
        m2.setPower(speed2 * power - P);
        m3.setPower(speed3 * power + P);
    }
}
