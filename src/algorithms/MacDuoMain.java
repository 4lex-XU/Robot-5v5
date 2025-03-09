package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters;


//============================================================================
//====================================MAIN====================================
//============================================================================

public class MacDuoMain extends MacDuoBaseBot {
	
	private static class Ennemy {
		double x, y;
	    double previousX, previousY;
	    double distance, direction, previousDirection;
	    Types type;

	    public Ennemy(double x, double y, double distance, double direction, Types type) {
	        this.x = x;
	        this.y = y;
	        this.distance = distance;
	        this.direction = direction;
	        this.previousDirection = direction;
	        this.previousX = x;
	        this.previousY = y;
	        this.type = type;
	    }
	    
	    public void updatePosition(double newX, double newY, double newDistance, double newDirection) {
	        this.previousX = this.x;
	        this.previousY = this.y;
	        this.previousDirection = this.direction;

	        this.x = newX;
	        this.y = newY;
	        this.distance = newDistance;
	        this.direction = newDirection;
	    }
	    
	    @SuppressWarnings("incomplete-switch")
		public double getSpeed() {
	    	double speed = 0;
	    	switch (type) {
	    		case OpponentMainBot :
	    			speed = Parameters.teamBMainBotSpeed;
	    			break;
	    		case OpponentSecondaryBot :	
	    			speed = Parameters.teamBSecondaryBotSpeed;
	    			break;
	    	}
			return speed;
	    }
	    public double predictX(double bulletTravelTime) {
	        double actualDirection = Math.atan2(y - previousY, x - previousX);
	        return x + Math.cos(actualDirection) * getSpeed() * bulletTravelTime;
	    }

//	    public double predictX(double bulletTravelTime) {
//	        return x + Math.cos(direction) * getSpeed() * bulletTravelTime;
//	    }
	    public double predictY(double bulletTravelTime) {
	        double actualDirection = Math.atan2(y - previousY, x - previousX);
	        return y + Math.sin(actualDirection) * getSpeed() * bulletTravelTime;
	    }
//	    public double predictY(double bulletTravelTime) {
//	        return y + Math.sin(direction) * getSpeed() * bulletTravelTime;
//	    }
	}
    private List<Ennemy> enemyTargets = new ArrayList<>();
	
	private static final double FIREANGLEPRECISION = 0.3;
    private static final double OBSTACLE_AVOIDANCE_DISTANCE = 100;
    private int fireStreak = 0;
    private static final int MAX_FIRE_STREAK = 10; 
    private Ennemy lastTarget = null; 
    
    //---VARIABLES---//
    private double rdvX, rdvY; 
    private double targetX, targetY;  
    private boolean fireOrder;
    private Parameters.Direction turnedDirection;
    
    private boolean obstacleDetected = false;
    private double obstacleDirection = 0;
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
	        	myPos = new Position((isTeamA ? Parameters.teamAMainBot1InitX : Parameters.teamBMainBot1InitX), 
				        			 (isTeamA ? Parameters.teamAMainBot1InitY : Parameters.teamBMainBot1InitY));
	            rdvX = isTeamA ? 300 : 2500;
	            rdvY = 1100;
                //sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
                break;
	        case MAIN2 : 
	        	myPos = new Position((isTeamA ? Parameters.teamAMainBot2InitX : Parameters.teamBMainBot2InitX),
	        						 (isTeamA ? Parameters.teamAMainBot2InitY : Parameters.teamBMainBot2InitY));
	            rdvX = isTeamA ? 450 : 2400;
	            rdvY = 1350;
                //sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
	            break;
	        case MAIN3 : 
	        	myPos = new Position((isTeamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX),
	        						 (isTeamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY));
	            rdvX = isTeamA ? 600 : 2300;
	            rdvY = 1700;
                //sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
	            break;
        }
	    isMoving = true;
	    state = State.FIRST_RDV;
	    rdv_point = true;
	    oldAngle = myGetHeading();
    }
    
    @Override
    public void step() {
    	//DEBUG MESSAGE
        boolean debug = true;
        if (debug && whoAmI == MAIN1) {
        	sendLogMessage("#MAIN1 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN2) {
        	sendLogMessage("#MAIN2 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN3) {
        	sendLogMessage("#MAIN3 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        
    	detection();	
		readMessages();
		if (state == State.FIRE) {
			handleFire();
			return;
		}

		if (freeze) return;
		
		if (getHealth() <= 0) {
			state = State.DEAD;
			allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false));
		}
		
		try {
			switch (state) {
				case FIRST_RDV:
					if (rdv_point) {
						reach_rdv_point(rdvX, rdvY);
					}
					break;
				case MOVING:
					reach_rdv_point(rdvX, rdvY);
					break;
				case MOVING_BACK:
					myMove(false);
					break;
				case TURNING_LEFT:
					turnLeft();
					break;
				case TURNING_RIGHT:
					turnRight();	
					break;	
				default:
					break;	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
	//=========================================ADDED=========================================	

    protected void detection() {
		freeze = false;
		fireOrder = false;
	    obstacleDetected = false;
        boolean enemyDetected = false;

        for (IRadarResult o : detectRadar()) {
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            double enemyX = myPos.getX() + o.getObjectDistance() * Math.cos(o.getObjectDirection());
	            double enemyY = myPos.getY() + o.getObjectDistance() * Math.sin(o.getObjectDirection());
	            broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            addOrUpdateEnemy(enemyX, enemyY, o.getObjectDistance(), o.getObjectDirection(), true, o.getObjectType());
	            if (!enemyDetected) {
	            	fireOrder = true;
                    enemyDetected = true;
                }
	        }
	    	if (o.getObjectType() == IRadarResult.Types.Wreck) {
	            double enemyX=myPos.getX()+o.getObjectDistance()*Math.cos(o.getObjectDirection());
	            double enemyY=myPos.getY()+o.getObjectDistance()*Math.sin(o.getObjectDirection());
	            broadcast("WRECK " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            //sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
	        }
	        
	        // Obstacle detection for movement
			obstacleDirection = o.getObjectDirection();
	        if ((o.getObjectDistance() <= OBSTACLE_AVOIDANCE_DISTANCE && ((isSameDirection(obstacleDirection, Parameters.NORTH) || isSameDirection(obstacleDirection, Parameters.SOUTH)))) 
					|| detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING ) {
				obstacleDetected = true;
	        	initiateObstacleAvoidance();
				//System.out.println("Obstacle detected at direction: " + (obstacleDirection * 180 / Math.PI) + "°");
	            //sendLogMessage("Obstacle detected at direction: " + (obstacleDirection * 180 / Math.PI) + "°");
	        }
	    }
	    
	    if (enemyDetected) {
	        state = State.FIRE;
	        sendLogMessage("state fire dans detection");
	        avoidanceTimer = 0;
	    } else if (obstacleDetected && state == State.MOVING) {
	        initiateObstacleAvoidance();
	    } else if (!enemyDetected && state != State.FIRE){
	        state = State.MOVING;
	        sendLogMessage("state moving dans detection");
	    }
	}
    
    private void initiateObstacleAvoidance() {
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
            //gMessage("Avoiding obstacle by turning right");
        } else {
            // Obstacle is on the left, turn right
        	turnedDirection = Parameters.Direction.LEFT;
            state = State.TURNING_LEFT;
            //sendLogMessage("Avoiding obstacle by turning left");
        }
        
        oldAngle = myGetHeading();
        turningTask = true;
        avoidanceTimer = AVOIDANCE_DURATION;
    }
    
    private void readMessages() {
    	sendLogMessage("readMessages");
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
            	case "ENEMY":
            		sendLogMessage(" ENEMY handleEnemyMessage");
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
	            	double distance = Math.sqrt(Math.pow(enemyX - myPos.getX(), 2) + Math.pow(enemyY - myPos.getY(), 2));
	            	if (distance < 700){
	            		state = State.MOVING_BACK;
	        	        sendLogMessage("state moving back dans readMessages");
	            		myMove(false);
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
    	allyPos.put(parts[1], new BotState(botX, botY, true));{
            if (parts[1].equals("SBOT")) {
                if (state != State.FIRE) {
                    rdvX = botX;
                    rdvY = botY;
                    //sendLogMessage(whoAmI + " following scout " + parts[1] + " to " + rdvX + ", " + rdvY);
                }
            }
    	}
    }
    
    private void handleWreckMessage(String[] parts) {
	    double wreckX = Double.parseDouble(parts[4]);
	    double wreckY = Double.parseDouble(parts[5]);

	    // Supprimer l'ennemi mort de la liste des cibles
	    enemyTargets.removeIf(enemy -> Math.abs(enemy.x - wreckX) < 50 && Math.abs(enemy.y - wreckY) < 50);

	    sendLogMessage("Enemy destroyed, len : " + enemyTargets.size());
	}
    
    private void handleEnemyMessage(String[] parts) {
    	sendLogMessage("handleEnemyMessage");
    	state = State.FIRE;
    	fireOrder = true;
        double enemyX = Double.parseDouble(parts[4]);
        double enemyY = Double.parseDouble(parts[5]);
        double enemyDistance = Double.parseDouble(parts[2]);
        double enemyDirection = Double.parseDouble(parts[1]);
        Types enemyType = parts[3].contains("MainBot") ? Types.OpponentMainBot : Types.OpponentSecondaryBot;

        addOrUpdateEnemy(enemyX, enemyY, enemyDistance, enemyDirection, false, enemyType);
        handleFire(); 
    }
    
    private void handleFire() {
        Ennemy target = chooseTarget();
        if (target != null) {
            targetX = target.x;
            targetY = target.y;
            if (target.distance > 950) {
                moveTowardsTarget(target);
                return; 
            }

            if (lastTarget != null && lastTarget.equals(target)) {
                fireStreak++;
                if (fireStreak >= MAX_FIRE_STREAK) {
                    sendLogMessage("Target stuck for too long, switching target.");
                    enemyTargets.remove(target);
                    fireStreak = 0;
                    lastTarget = null;

                    target = chooseTarget();
                    if (target == null) {
                        sendLogMessage("No more targets, switching to MOVING.");
                        state = State.MOVING; 
                        fireOrder = false;
                        return;
                    } else {
                        targetX = target.x;
                        targetY = target.y;
                        lastTarget = target;
                    }
                }
            } else {
                fireStreak = 0; 
                lastTarget = target;
            }

            if (!enemyTargets.contains(target)) {
                sendLogMessage("Target eliminated, stopping fire.");
                fireOrder = false;
                state = State.MOVING;
                return;
            }

            if (fireOrder) {
            	if (target.getSpeed() > 1 || target.distance > 600) {
            	    firePositionWithPrediction(target);
            	} else {
            	    firePosition(target.x, target.y);
            	}
            }
        } else {
            state = State.MOVING;
            sendLogMessage("state moving dans handleFire");
            fireOrder = false;
        }
    }

	protected void myMove(boolean forward) {
		if (forward) {
			// If we're in avoidance mode
			if (avoidanceTimer > 0) {
				avoidanceTimer--;
				if (avoidanceTimer == 0) {
					state = State.MOVING;
					 sendLogMessage("state moving dans myMove"); // Back to normal movement when avoidance complete
				}
				return;
			}
			
			// Regular movement when no active avoidance
			if (!rdv_point && (obstacleDetected || detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING)) {
				initiateObstacleAvoidance();
				return;
			}
			
			double myPredictedX = myPos.getX() + Math.cos(getHeading()) * Parameters.teamAMainBotSpeed;
			double myPredictedY = myPos.getY() + Math.sin(getHeading()) * Parameters.teamAMainBotSpeed;

			// évite de se bloquer dans les murs
			if(myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900 ) {
				move(); 
				myPos.setX(myPredictedX);
				myPos.setY(myPredictedY);
				sendMyPosition();
				return;
			}
		} else {
			// Backward movement
			double myPredictedX = myPos.getX() - Math.cos(getHeading()) * Parameters.teamAMainBotSpeed;
			double myPredictedY = myPos.getY() - Math.sin(getHeading()) * Parameters.teamAMainBotSpeed;

			// évite de se bloquer dans les murs
			if(myPredictedX > 100 && myPredictedX < 2900 && myPredictedY > 100 && myPredictedY < 1900 ) {
				moveBack();
				myPos.setX(myPredictedX);
				myPos.setY(myPredictedY);
				sendMyPosition();
				return;
			}
		}
	}
	
	private void firePosition(double x, double y) {
	    double angle = Math.atan2(y - myPos.getY(), x - myPos.getX());
	    fire(angle);
	}
	
	private void firePositionWithPrediction(Ennemy target) {
	    double bulletSpeed = Parameters.bulletVelocity; 
	    double distanceToEnemy = target.distance;
	    // Temps que met la balle pour atteindre l'ennemi
	    double bulletTravelTime = distanceToEnemy / bulletSpeed;
	    double predictedX = target.predictX(bulletTravelTime);
	    double predictedY = target.predictY(bulletTravelTime);

	    double angle = Math.atan2(predictedY - myPos.getY(), predictedX - myPos.getX());
	    sendLogMessage("Firing at predicted position: (" + (int) predictedX + ", " + (int) predictedY + ")");
	    fire(angle);
	}


	private boolean isRoughlySameDirection(double dir1, double dir2) {
	    return Math.abs(normalize(dir1) - normalize(dir2)) < FIREANGLEPRECISION;
	}
	
	private void addOrUpdateEnemy(double x, double y, double distance, double direction, boolean isMyDetection, Types type) {
	    if (!isMyDetection) {
	        double dx = x - myPos.getX();
	        double dy = y - myPos.getY();
	        distance = Math.sqrt(dx * dx + dy * dy); 
	        direction = Math.atan2(dy, dx);
	    }

	    // Vérifier si cet ennemi est déjà dans la liste et le mettre à jour
	    for (Ennemy enemy : enemyTargets) {
	        if (Math.abs(enemy.x - x) < 50 && Math.abs(enemy.y - y) < 50) {
	        	enemy.updatePosition(x, y, distance, direction);
	            return;
	        }
	    }
	    enemyTargets.add(new Ennemy(x, y, distance, direction, type));
	}

	
	private Ennemy chooseTarget() {
	    // Trie les ennemis par distance croissante
	    Collections.sort(enemyTargets, (e1, e2) -> Double.compare(e1.distance, e2.distance));
	    sendLogMessage("choosetarget target len : " + enemyTargets.size());
	    // Parcours les ennemis et choisit celui qui est le plus proche sans allié sur la trajectoire
	    for (Ennemy enemy : enemyTargets) {
	        boolean allyInTheWay = false;
	        for (BotState ally : allyPos.values()) {
	            if (!ally.isAlive()) continue; 

	            double allyX = ally.getPosition().getX();
	            double allyY = ally.getPosition().getY();
	            double allyDirection = Math.atan2(allyY - myPos.getY(), allyX - myPos.getX());
	            double enemyDirection = Math.atan2(enemy.y - myPos.getY(), enemy.x - myPos.getX());
	            boolean isSameLine = isRoughlySameDirection(allyDirection, enemyDirection);
	            // Vérifie si l'allié est **entre** le robot et l'ennemi
	            double distAlly = Math.sqrt(Math.pow(allyX - myPos.getX(), 2) + Math.pow(allyY - myPos.getY(), 2));
	            double distEnemy = enemy.distance;

	            if (isSameLine && distAlly < distEnemy) {
	            	allyInTheWay = true;
	                sendLogMessage("(" + allyX + ", " + allyY + ") aligned with enemy at (" + enemy.x + ", " + enemy.y + ")");                  
	                break;
	            }
	        }
	        
	        if (!allyInTheWay) {
	        	sendLogMessage("ennemy");
	            return enemy;
	        }
	    }
	    return null;
	}
	
	private void moveTowardsTarget(Ennemy target) {
	    sendLogMessage("moveTowardsTarget | Current Pos: (" + myPos.getX() + ", " + myPos.getY() + ") | Target Pos: (" + target.x + ", " + target.y + ")");

	    double angleToTarget = Math.atan2(target.y - myPos.getY(), target.x - myPos.getX());
	    double currentHeading = getHeading();

	    if (!isRoughlySameDirection(currentHeading, angleToTarget)) {
	        sendLogMessage("Turning towards target | Current Heading: " + Math.toDegrees(currentHeading) + "° | Target Angle: " + Math.toDegrees(angleToTarget) + "°");
	        turnTo(angleToTarget);  
	        return;  // On arrête ici pour laisser le temps de tourner
	    }

	    sendLogMessage("Aligned with target. Moving forward...");
	    myMove(true);
	}

}