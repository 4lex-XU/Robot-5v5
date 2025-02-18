package algorithms.Stage1;

import algorithms.AbstractBrain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;

public class ExplorerBot extends AbstractBrain{
    private static final int TURN_TO_NORTH = 0, MOVING = 1, TURNING = 2;

    public ExplorerBot() { super(); }
  
    public void activate() {
      state = TURN_TO_NORTH;
    }
  
    public void step() {
        // Étape 1 : Se tourner vers le nord
      if (state == TURN_TO_NORTH) {
        if (!isSameDirection(getHeading(), Parameters.NORTH)) {
          stepTurn(Parameters.Direction.LEFT);
          sendLogMessage("Turning left to face North. Current Heading: " + getHeading());
        } else {
          state = MOVING;
          sendLogMessage("Now facing North. Starting to move.");
        }
        return;
      }
  
      // Étape 2 : Avancer en ligne droite
      if (state == MOVING) {
        IFrontSensorResult frontSensor = detectFront();
        if (frontSensor != null && frontSensor.getObjectType() == IFrontSensorResult.Types.WALL) {
          // Log when a wall is detected in front of the robot
          sendLogMessage("Wall detected at front. Stopping and preparing to turn.");
          state = TURNING;
          oldAngle = normalize(getHeading());
        } else {
          myMove();
          sendLogMessage("Moving forward. No wall detected.");
        }
        return;
      }
  
      // Étape 3 : Tourner à droite au mur
      if (state == TURNING) {
        if (isHeading(normalize(oldAngle + Parameters.RIGHTTURNFULLANGLE))) {
          state = MOVING;
          sendLogMessage("Turn completed. Moving forward.");
        } else {
          stepTurn(Parameters.Direction.RIGHT);
          sendLogMessage("Turning right. Current Heading: " + getHeading());
        }
        return;
      }
    }
  }