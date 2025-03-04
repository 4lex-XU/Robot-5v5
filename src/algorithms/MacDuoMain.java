package algorithms;

import java.util.ArrayList;

import algorithms.MacDuoBaseBot.State;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;


//============================================================================
//====================================MAIN====================================
//============================================================================

public class MacDuoMain extends MacDuoBaseBot {
	
	private static final double FIREANGLEPRECISION = Math.PI/(double)6;
    private static final double OBSTACLE_AVOIDANCE_DISTANCE = 150;
    private static final double ENEMY_FIRING_RANGE = 900;
    
    // les ids des shooters
    private static final String MAIN1 = "1";
    private static final String MAIN2 = "2";
    private static final String MAIN3 = "3";
    
    
    //---VARIABLES---//
    private double rdvX, rdvY; 
    private double targetX, targetY;  
    private double followTargetX, followTargetY; 
    private boolean friendlyFire;
    private boolean fireOrder;
    private int fireRythm,rythm,counter;
    private int countDown;
    private Parameters.Direction turnedDirection;
    private int fireCounter = 0;
    private static final int MAX_FIRE_COUNT = 5; // Ajustez selon vos besoins
    
    private boolean obstacleDetected = false;
    private double obstacleDirection = 0;
    private double avoidanceAngle = Math.PI/2;
    private int avoidanceTimer = 0;
    private static final int AVOIDANCE_DURATION = 10;

 	
	//=========================================CORE=========================================	

	public MacDuoMain () { super();}
    
    @Override
    public void activate() {
		isTeamA = isHeading(Parameters.EAST);

    	boolean top = false;
    	boolean bottom = false;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) top = true;
            else if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH)) bottom = true;
        
        whoAmI = MAIN3;
        if (top && bottom) whoAmI = MAIN2;
        else if (!top && bottom) whoAmI = MAIN1; 
                
        switch(whoAmI) {
	        case MAIN1 : 
	        	myX = isTeamA ? Parameters.teamAMainBot1InitX : Parameters.teamBMainBot1InitX;
	            myY = isTeamA ? Parameters.teamAMainBot1InitY : Parameters.teamBMainBot1InitY;
	            rdvX = isTeamA ? 300 : 2500;
	            rdvY = 1100;
                sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
                break;
	        case MAIN2 : 
	        	myX = isTeamA ? Parameters.teamAMainBot2InitX : Parameters.teamBMainBot2InitX;
	            myY = isTeamA ? Parameters.teamAMainBot2InitY : Parameters.teamBMainBot2InitY;
	            rdvX = isTeamA ? 450 : 2400;
	            rdvY = 1350;
                sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
	            break;
	        case MAIN3 : 
	        	myX = isTeamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX;
	            myY = isTeamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY;
	            rdvX = isTeamA ? 600 : 2300;
	            rdvY = 1700;
                sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
	            break;
        }
	    isMoving = true;
	    state = State.FIRST_RDV;
	    rdv_point = true;
	    fireRythm = 0;
	    oldAngle = myGetHeading();
    }
    
    @Override
    public void step() {
    	//DEBUG MESSAGE
        boolean debug = true;
        if (debug && whoAmI == MAIN1) {
        	sendLogMessage("#MAIN1 *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN2) {
        	sendLogMessage("#MAIN2 *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN3) {
        	sendLogMessage("#MAIN3 *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        
    	detection();	
		if (state == State.FIRE) {
			handleFire();
			return;
		}
		readMessages();
		if (freeze) return;
		
		if (getHealth() <= 0) {
			state = State.DEAD;
		}
		
		switch (state) {
			case FIRST_RDV:
				if (rdv_point) {
					reach_rdv_point(rdvX, rdvY);
				}
				break;
			case MOVING:
				reach_rdv_point(rdvX, rdvY);
				//myMove();
				break;
			case MOVING_BACK:
				moveBack();
				break;
			case TURNING_LEFT:
				turnLeft();
				break;
			case TURNING_RIGHT:
				turnRight();	
				break;		
		}
    }
    
	//=========================================ADDED=========================================	

    protected void detection() {
		freeze = false;
	    friendlyFire = true;
	    obstacleDetected = false;
        boolean enemyDetected = false;
        double enemyDistance = Double.MAX_VALUE;
        double enemyDirection = 0;

		// Détection des ennemis et des obstacles
	    for (IRadarResult o : detectRadar()) {
	    	// Enemy detection - highest priority
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            // Transmettre la position des ennemis : ENEMY dir dist type enemyX enemyY
	            double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
	            double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
	            broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            
	            // Track closest enemy
	            if (!enemyDetected || o.getObjectDistance() < enemyDistance) {
                    enemyDetected = true;
                    enemyDirection = o.getObjectDirection();
                    enemyDistance = o.getObjectDistance();
                    targetX = enemyX;
                    targetY = enemyY;
                }
	        }
	    	if (o.getObjectType() == IRadarResult.Types.Wreck) {
	            double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
	            double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
	            broadcast("WRECK " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            //sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
	        }

	        // Friendly fire check
	        if (o.getObjectType() == IRadarResult.Types.TeamMainBot || 
	            o.getObjectType() == IRadarResult.Types.TeamSecondaryBot || 
	            o.getObjectType() == IRadarResult.Types.Wreck) {
	            if (fireOrder && onTheWay(o.getObjectDirection())) {
	                friendlyFire = false;
	            }
	        }
	        
	        // Obstacle detection for movement
	        if (o.getObjectDistance() <= OBSTACLE_AVOIDANCE_DISTANCE && detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING ) {
	        	moveBack();
	        	initiateObstacleAvoidance();
	            obstacleDetected = true;
	            obstacleDirection = o.getObjectDirection();
	            sendLogMessage("Obstacle detected at direction: " + (obstacleDirection * 180 / Math.PI) + "°");
	        }
	    }
	    
	    // Set state based on detection priorities
	    if (enemyDetected) {
	        freeze = true;
	        fireOrder = true;
	        state = State.FIRE;
	        avoidanceTimer = 0;
	    } else if (obstacleDetected && state == State.MOVING) {
	        initiateObstacleAvoidance();
	    } else {
	        state = State.MOVING; // Ensure state is MOVING if no enemy or obstacle is detected
	    }
	}
    
    private void initiateObstacleAvoidance() {
        // Determine which way to turn based on obstacle direction
        double relativeAngle = normalize(obstacleDirection - getHeading());
        if (turnedDirection != null) {
	        if (turnedDirection == Parameters.Direction.RIGHT) {
	        	state = State.TURNING_RIGHT;
	        }
	        else {
	        	 state = State.TURNING_LEFT;
	        }
        }
        else if (relativeAngle > 0 && relativeAngle < Math.PI) {
            // Obstacle is on the right, turn left
        	turnedDirection = Parameters.Direction.RIGHT;
        	state = State.TURNING_RIGHT;
            sendLogMessage("Avoiding obstacle by turning right");
        } else {
            // Obstacle is on the left, turn right
        	turnedDirection = Parameters.Direction.LEFT;
            state = State.TURNING_LEFT;
            sendLogMessage("Avoiding obstacle by turning left");
        }
        
        oldAngle = myGetHeading();
        turningTask = true;
        avoidanceTimer = AVOIDANCE_DURATION;
    }
    
    private void readMessages() {
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
            	case "ENEMY":
	                handleEnemyMessage(parts);
	                break;
                case "WRECK" :
                	handleWreckMessage(parts);
                	break;
	            case "POS":
	            	handlePosMessage(parts);
	            	break;
	            case "MOVING_BACK":
	            	double enemyX = Double.parseDouble(parts[2]);
	                double enemyY = Double.parseDouble(parts[3]);
	            	double distance = Math.sqrt(Math.pow(enemyX - myX, 2) + Math.pow(enemyY - myY, 2));
	            	if (distance < 700){
	            		state = State.MOVING_BACK;
	            		moveBack();
	            	}
	            	break;
                case "SCOUT_DOWN_A":
                case "SCOUT_DOWN_B":
                    break;
            }
        }
    }

    private void handlePosMessage(String[] parts) {
    	double botX = Double.parseDouble(parts[2]);
        double botY = Double.parseDouble(parts[3]);
    	allyPos.put(parts[1], new Double[]{botX, botY});{
            if (parts[1].equals("SBOT")) {
                if (state != State.FIRE) {
                    rdvX = botX;
                    rdvY = botY;
                    sendLogMessage(whoAmI + " following scout " + parts[1] + " to " + rdvX + ", " + rdvY);
                }
            }
    	}
    }
    
    private void handleWreckMessage(String[] parts) {
    	state = State.MOVING;
		targetX = 0;
		targetY = 0;
		fireOrder = true;
	}
    
    private void handleEnemyMessage(String[] parts) {
    	fireOrder = true;
        double enemyX = Double.parseDouble(parts[4]);
        double enemyY = Double.parseDouble(parts[5]);
        
     // Calculer la direction de l'ennemi
        double enemyDirection = Math.atan((enemyY-myY)/(double)(enemyX-myX));

        // Vérifier si un coéquipier est dans la direction de l'ennemi
        for (Double[] allyPosition : allyPos.values()) {
            double allyX = allyPosition[0];
            double allyY = allyPosition[1];
            double allyDirection = Math.atan((allyY-myY)/(double)(allyX-myX));
            if (isRoughlySameDirection(allyDirection, enemyDirection)) {
            	sendLogMessage("ally is found in this direction can not fire");
            	friendlyFire = false;
            	state = State.MOVING;
                return;
            }
        }
        state = State.FIRE;
        targetX = enemyX;
        targetY = enemyY;
        handleFire();
    }
    
    private void handleFire() {
        // Vérifier si on a bien une cible
        if (targetX != 0 && targetY != 0) {
            double dx = targetX - myX;
            double dy = targetY - myY;

            if (fireOrder && friendlyFire) {
                firePosition(targetX, targetY);
            }

            // Vérifier la distance après tir
            double distanceEnemyMe = Math.sqrt(dx * dx + dy * dy);
            if (distanceEnemyMe > ENEMY_FIRING_RANGE) {
                state = State.MOVING;
                fireOrder = false;
            }
        } else {
            state = State.MOVING;
            fireOrder = false;
        }
    }
    
	protected void myMove() {
	    // If we're in avoidance mode
	    if (avoidanceTimer > 0) {
	        avoidanceTimer--;
	        if (avoidanceTimer == 0) {
	            state = State.MOVING; // Back to normal movement when avoidance complete
	        }
	        return;
	    }
	    
	    // Regular movement when no active avoidance
	    if (!rdv_point && (obstacleDetected || detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING)) {
	        initiateObstacleAvoidance();
	        return;
	    }
	    
	    double myPredictedX = myX + Math.cos(getHeading()) * Parameters.teamAMainBotSpeed;
		double myPredictedY = myY + Math.sin(getHeading()) * Parameters.teamAMainBotSpeed;

		// évite de se bloquer dans les murs
		if(myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900 ) {
			move(); 
            myX = myPredictedX;
            myY = myPredictedY;
    		sendMyPosition();
    		return;
		}
	}
	
	private void firePosition(double x, double y) {
	    if (myX <= x) {
	        fire(Math.atan((y-myY)/(double)(x-myX)));
	    } else {
	        fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
	    }
	}
	
	private boolean isRoughlySameDirection(double dir1, double dir2) {
	    return Math.abs(normalize(dir1) - normalize(dir2)) < FIREANGLEPRECISION;
	}
	
	private boolean onTheWay(double angle) {
	    if (myX <= targetX) {
	        return isRoughlySameDirection(angle, Math.atan((targetY-myY)/(double)(targetX-myX)));
	    } else {
	        return isRoughlySameDirection(angle, Math.PI + Math.atan((targetY-myY)/(double)(targetX-myX)));
	    }
	}
}