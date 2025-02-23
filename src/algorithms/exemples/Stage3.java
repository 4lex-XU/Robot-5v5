package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class Stage3 extends Brain {
    private enum State { INIT, TURNING, GO_UP, GO_RIGHT, GO_DOWN, GO_LEFT, SPIN, DEFAULT }
    private State state;
    private Parameters.Direction nextTurnDirection;
    private double targetHeading;
    private boolean isMoving;
    private double myX, myY;
    private int whoAmI;

    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final double ANGLEPRECISION = 0.04;
    private static final double SOUTH_BORDER_Y = 3050; //il faut remplacer par la bonne valeur de height 
    private static final double[] CHECKPOINTS = { SOUTH_BORDER_Y * 0.25, SOUTH_BORDER_Y * 0.5, SOUTH_BORDER_Y * 0.75 };
    private boolean isSpinning;
    
    public Stage3() { 
        super();
        state = State.INIT;
    }

    public void activate() {
        whoAmI = ROCKY;
        for (IRadarResult o: detectRadar()) {
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) {
                whoAmI = MARIO;
            }
        }

        if (whoAmI == ROCKY) {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
        } else {
            myX = 0;
            myY = 0;
        }

        state = (whoAmI == ROCKY) ? State.INIT : State.DEFAULT;
        isMoving = false;
        isSpinning = false;
    }

    public void step() {
    	
        if (isMoving && whoAmI == ROCKY){
        	if (detectFront().getObjectType()!=IFrontSensorResult.Types.WALL) {
	            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
	            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
	            isMoving=false;
          }
        }
        
        if (whoAmI == MARIO) {
            defaultBehavior();
            return;
        }

        switch (state) {
            case INIT:
            	if (! isSameDirection(Parameters.NORTH, getHeading())) {
                    stepTurn(Parameters.Direction.LEFT);
                } else {
                    state = State.GO_UP;
                }
                break;

            case TURNING:
                if (! isSameDirection(targetHeading, getHeading())) {
                    stepTurn(nextTurnDirection);
                } else {
                    updateStateAfterTurn();
                }
                break;

            case GO_UP:
                if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
                    setTurnState(Parameters.EAST, Parameters.Direction.RIGHT);
                } else {
                	isMoving = true;
                    move();
                }
                break;

            case GO_RIGHT:
                if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
                    setTurnState(Parameters.SOUTH, Parameters.Direction.RIGHT);
                } else {
                	isMoving = true;
                    move();
                }
                break;

            case GO_DOWN:
                if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
                    setTurnState(Parameters.WEST, Parameters.Direction.RIGHT);
                } else {
                	isMoving = true;
                    move();
                }
                break;

            case GO_LEFT:
                if (detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
                    setTurnState(Parameters.NORTH, Parameters.Direction.RIGHT);
                } else {
                	isMoving = true;
                    move();
                    checkForSpin();
                }
                break;
            case SPIN:
                if (!isSpinning) {
                    isSpinning = true;
                    isMoving = false;
                    targetHeading = getHeading(); // Conserver l'orientation initiale
                    nextTurnDirection = Parameters.Direction.RIGHT;
                    state = State.TURNING;
                } else {
                    // Vérifier si une rotation complète a été effectuée
                    if (isCompleteRotation()) {
                        // La rotation est terminée
                        isSpinning = false;
                        setTurnState(Parameters.WEST, Parameters.Direction.RIGHT);
                    } else {
                        // Continuer la rotation
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                }
                break;

            case DEFAULT:
                move();
                break;
        }
    }
    
    private void checkForSpin() {
        for (double checkpoint : CHECKPOINTS) {
            if (Math.abs(myX - checkpoint) < 10.0) {
                state = State.SPIN;
                return;
            }
        }
    }
    
    private boolean isCompleteRotation() {
        double currentHeading = getHeading();
        double initialHeading = targetHeading;
        
        // Calculer la différence angulaire
        double angleDiff = normalize(currentHeading - initialHeading);
        
        // Une rotation complète fait 360 degrés (2*PI radians)
        return angleDiff > (2 * Math.PI - ANGLEPRECISION);
    }

    private void setTurnState(double newHeading, Parameters.Direction turnDirection) {
        targetHeading = newHeading;
        nextTurnDirection = turnDirection;
        state = State.TURNING;
    }

    private void updateStateAfterTurn() {
        if (isSameDirection(targetHeading, Parameters.EAST)) {
            sendLogMessage("Changement d'état: GO_RIGHT");
            state = State.GO_RIGHT;
        } else if (isSameDirection(targetHeading, Parameters.SOUTH)) {
            sendLogMessage("Changement d'état: GO_DOWN");
            state = State.GO_DOWN;
        } else if (isSameDirection(targetHeading, Parameters.WEST)) {
            sendLogMessage("Changement d'état: GO_LEFT");
            state = State.GO_LEFT;
        } else if (isSameDirection(targetHeading, Parameters.NORTH)) {
            sendLogMessage("Changement d'état: GO_UP");
            state = State.GO_UP;
        } else {
        	double minDiff = Double.MAX_VALUE;
            State newState = State.GO_UP;
            
            double[] directions = {Parameters.NORTH, Parameters.EAST, Parameters.SOUTH, Parameters.WEST};
            State[] states = {State.GO_UP, State.GO_RIGHT, State.GO_DOWN, State.GO_LEFT};
            
            for (int i = 0; i < directions.length; i++) {
                double diff = Math.abs(normalize(targetHeading) - normalize(directions[i]));
                if (diff < minDiff) {
                    minDiff = diff;
                    newState = states[i];
                }
            }
            sendLogMessage("Direction la plus proche trouvée: " + newState);
            state = newState;
        }
    }

    private void defaultBehavior() {
        if (detectFront().getObjectType() != IFrontSensorResult.Types.WALL) {
            move();
            return;
        }

        stepTurn(Parameters.Direction.RIGHT);
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(normalize(dir1) - normalize(dir2)) < ANGLEPRECISION;
    }

    private double normalize(double dir) {
        double res = dir;
        while (res < 0) res += 2 * Math.PI;
        while (res >= 2 * Math.PI) res -= 2 * Math.PI;
        return res;
    }
}
