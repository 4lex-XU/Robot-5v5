package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import algorithms.MacDuoBaseBot.State;
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
	    double speed;
	    boolean hasMoved;

	    public Ennemy(double x, double y, double distance, double direction, Types type) {
	        this.x = x;
	        this.y = y;
	        this.distance = distance;
	        this.direction = direction;
	        this.previousDirection = direction;
	        this.previousX = x;
	        this.previousY = y;
	        this.type = type;
	        this.speed = 0;
	        this.hasMoved = false; // First detection, no movement yet
	    }
	    
	    public void updatePosition(double newX, double newY, double newDistance, double newDirection) {
	        this.previousX = this.x;
	        this.previousY = this.y;
	        this.previousDirection = this.direction;

	        this.x = newX;
	        this.y = newY;
	        this.distance = newDistance;
	        this.direction = newDirection;

	        // Calculate speed based on movement between two steps
	        double dx = x - previousX;
	        double dy = y - previousY;
	        this.speed = Math.sqrt(dx * dx + dy * dy); // Distance moved in one step
	        this.hasMoved = true; // We now have two positions to predict from
	    }
	    
	    public double predictX(double bulletTravelTime) {
	        if (!hasMoved) {
	            return x; // No movement data yet, use current position
	        }
	        double actualDirection = Math.atan2(y - previousY, x - previousX);
	        return x + Math.cos(actualDirection) * speed * bulletTravelTime;
	    }

	    public double predictY(double bulletTravelTime) {
	        if (!hasMoved) {
	            return y; // No movement data yet, use current position
	        }
	        double actualDirection = Math.atan2(y - previousY, x - previousX);
	        return y + Math.sin(actualDirection) * speed * bulletTravelTime;
	    }
	    
	}
	
    private List<Ennemy> enemyTargets = new ArrayList<>();
    private List<double[]> wreckPositions = new ArrayList<>();
    private List<Ennemy> enemyPosToAvoid = new ArrayList<>();
	
	private static final double FIREANGLEPRECISION = 0.3;
    private static final double OBSTACLE_AVOIDANCE_DISTANCE = 100;
    private int fireStreak = 0;
    private static final int MAX_FIRE_STREAK = 10; 
    private Ennemy lastTarget = null; 
    
    //---VARIABLES---//
    private double rdvX = 0.0;
    private double rdvY = 0.0; 
    private boolean fireOrder;
    private Parameters.Direction turnedDirection;
    private boolean avoidingEnnemy = false;
    
    private boolean obstacleDetected = false;
    private boolean obstacleInWay = false;
    private double obstacleDirection = 0;
    private int waitingTimer = 0;
    private static final int WAITING_DURATION = 1000;
    private Ennemy target;
	private int fireStrike = 0;
	private static final int MAX_FIRESTRIKE = 20;
    
	//=========================================CORE=========================================	

	public MacDuoMain () { super();}
    
    @Override
    public void activate() {
		isTeamA = (getHeading() == Parameters.EAST);
  
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
//	            rdvX = isTeamA ? 300 : 2500;
//	            rdvY = 1100;
                //sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
                break;
	        case MAIN2 : 
	        	myPos = new Position((isTeamA ? Parameters.teamAMainBot2InitX : Parameters.teamBMainBot2InitX),
	        						 (isTeamA ? Parameters.teamAMainBot2InitY : Parameters.teamBMainBot2InitY));
//	            rdvX = isTeamA ? 450 : 2400;
//	            rdvY = 1350;
                //sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
	            break;
	        case MAIN3 : 
	        	myPos = new Position((isTeamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX),
	        						 (isTeamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY));
//	            rdvX = isTeamA ? 600 : 2300;
//	            rdvY = 1700;
                //sendLogMessage("targetX and targetY : " + rdvX + ", " + rdvY);
	            break;
        }
	    state = State.FIRST_RDV;
//	    rdv_point = true;
	    oldAngle = myGetHeading();
	    
    }
    
    @Override
    public void step() {
    	//DEBUG MESSAGE
        boolean debug = true;
        if (debug && whoAmI == MAIN1) {
        	//sendLogMessage("#MAIN1 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN2) {
        	//sendLogMessage("#MAIN2 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        if (debug && whoAmI == MAIN3) {
        	sendLogMessage("#MAIN3 *thinks* (x,y)= ("+(int)myPos.getX()+", "+(int)myPos.getY()+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"°. #State= "+state);
        }
        
    	detection();
		readMessages();
		if (getHealth() <= 0) {
			state = State.DEAD;
			allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false));
			return;
		}
        target = chooseTarget();
        if(target!=null) {
        	state=State.FIRE;
        	if (fireStrike == 0) { 
        		lastTarget = target;
        	}
        }

		try {
			switch (state) {
				case FIRE:
					//System.out.println(whoAmI + " " + isShooterAvoiding + " TEAMA " + isTeamA);
			        handleFire(target);
			        break;
				
				case FIRST_RDV:
					if (rdvX != 0.0 && rdvY != 0.0 ) {
						reach_rdv_point(rdvX, rdvY);
					}
					break;
				case MOVING:
					boolean following = (allyPos.get(SBOT).isAlive() || allyPos.get(NBOT).isAlive());
					if (following) {
						//System.out.println(" FOLLOWINGGGGGGGGGGGGGGGG " + whoAmI + " team AAA " + isTeamA);
						if (!isShooterAvoiding) {
							reach_rdv_point(rdvX, rdvY);
							
						}
						else {
							if (!hasReachedTarget(targetX, targetY, true)) {
					            myMove(true);
					        } else {
					            // La cible d'évitement est atteinte
					            isShooterAvoiding = false;
					        }
						}
					} else {
						//System.out.println("  NOTTTTTTT FOLLOWINGG " + whoAmI + " team AAA " + isTeamA);
						if (waitingTimer < WAITING_DURATION) {
							waitingTimer ++;
							return;
						}
						//waitingTimer = 0;
						myMove(true);
					}
				    break;

				case MOVING_BACK:
				    // Ici, on recule jusqu'à atteindre la cible calculée pour le recul
				    if (!hasReachedTarget(targetX, targetY, false)) {
				        myMove(false);
				    } else {
				        // Une fois la cible atteinte, on peut par exemple relancer l'évitement ou passer à un autre état
				        initiateObstacleAvoidance();
				    }
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
		fireOrder = false;
	    obstacleDetected = false;
        boolean enemyDetected = false;

        for (IRadarResult o : detectRadar()) {
			double oX = myPos.getX() + o.getObjectDistance() * Math.cos(o.getObjectDirection());
			double oY = myPos.getY() + o.getObjectDistance() * Math.sin(o.getObjectDirection());
			if (allyPos.get(whoAmI).isAlive() && o.getObjectType() != IRadarResult.Types.BULLET) {
				
				if (state == State.MOVING_BACK) {
					for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
				        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), normalize(getHeading() + Math.PI), p.getX(), p.getY());
						//System.out.println(whoAmI + " " + myPos.getX()+ " " + myPos.getY() + " || "+ p.getX() + " " + p.getY());
	
				        if (obstacleInPath) {
				            //sendLogMessage("Obstacle détecté dans la trajectoire circulaire !");
				            //System.out.println("Obstacle détecté");
				            obstacleDetected = true;
				            obstacleDirection = o.getObjectDirection();
				            initiateObstacleAvoidance();
				        } 
				    }
				} 
				else {
					for (Position p : getObstacleCorners(o, myPos.getX(), myPos.getY())) {
				        boolean obstacleInPath = isPointInTrajectory(myPos.getX(), myPos.getY(), getHeading(), p.getX(), p.getY());
						//System.out.println(whoAmI + " " + myPos.getX()+ " " + myPos.getY() + " || "+ p.getX() + " " + p.getY());
	
				        if (obstacleInPath) {
				            //System.out.println("Obstacle détecté dans la trajectoire circulaire " + whoAmI + " TEAM AAAA " +  isTeamA);
				            //System.out.println("Obstacle détecté");
				            obstacleDetected = true;
				            obstacleDirection = o.getObjectDirection();
				            initiateObstacleAvoidance();
				        } 
				    }
				}
			}		    
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + oX + " " + oY);
	            addOrUpdateEnemy(oX, oY, o.getObjectDistance(), o.getObjectDirection(), true, o.getObjectType());
	            if (!enemyDetected) {
	            	fireOrder = true;
                    enemyDetected = true;
                }
	        }
	    	if (o.getObjectType() == IRadarResult.Types.Wreck) {
	            broadcast("WRECK " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + oX + " " + oY);
	            handleWreckMessage(new String[]{"WRECK", "", "", "", String.valueOf(oX), String.valueOf(oY)});
	            //sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
	        }
	    }
	    	    //if (enemyDetected && !isShooterAvoiding) {
	       // state = State.FIRE;
	        //sendLogMessage("state fire dans detection");
	    if (!enemyDetected && state != State.FIRE && !isShooterAvoiding){
	        state = State.MOVING;
	        //sendLogMessage("state moving dans detection");
	    }
	}
    
    private void readMessages() {
    	//sendLogMessage("readMessages");
        ArrayList<String> messages = fetchAllMessages();
		ArrayList<String> ennemyMessages = new ArrayList();

        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
            	case "ENEMY":
            		//sendLogMessage("ENEMY handleEnemyMessage");
					ennemyMessages.add(msg);
	                break;
                case "WRECK" :
                	handleWreckMessage(parts);
                	break;
	            case "POS":
	            	handlePosMessage(parts);
	            	break;
				case "DEAD":
					allyPos.put(parts[1], new BotState( allyPos.get(parts[1]).getPosition().getX(), 
														allyPos.get(parts[1]).getPosition().getY(), 
														false));
					break;
            }
        }

		for (String msg : ennemyMessages) {
			String[] parts = msg.split(" ");
			handleEnemyMessage(parts);
		}
    }

    private void handlePosMessage(String[] parts) {
    	double botX = Double.parseDouble(parts[2]);
        double botY = Double.parseDouble(parts[3]);
    	allyPos.put(parts[1], new BotState(botX, botY, true));
    	sendLogMessage(parts[1] + " position " + botX + " " +  botY);
            if (parts[1].equals("SBOT")) {
                if (state != State.FIRE) {
                    rdvX = botX;
                    rdvY = botY;
                    //sendLogMessage(whoAmI + " following scout " + parts[1] + " to " + rdvX + ", " + rdvY);
                }
            } else if (!allyPos.get(SBOT).isAlive() && parts[1].equals("NBOT")) {
				if (state != State.FIRE) {
					rdvX = botX;
					rdvY = botY;
					sendLogMessage(whoAmI + " following scout " + parts[1] + " to " + rdvX + ", " + rdvY);
				}
			}
    }
    
    
    private void handleWreckMessage(String[] parts) {
	    double wreckX = Double.parseDouble(parts[4]);
	    double wreckY = Double.parseDouble(parts[5]);

	    boolean exists = false;
	    for (double[] wreck : wreckPositions) {
	        if (Math.abs(wreck[0] - wreckX) < 20 && Math.abs(wreck[1] - wreckY) < 20) {
	            exists = true;
	            break;
	        }
	    }
	    if (!exists) {
	        wreckPositions.add(new double[]{wreckX, wreckY});
	        //sendLogMessage("New wreck detected at (" + (int) wreckX + ", " + (int) wreckY + ")");
	    }
	    
	    Ennemy detectedEnemyWreck = null;
	    for (Ennemy enemy : enemyTargets) {
	    	if (Math.abs(enemy.x - wreckX) < 50 && Math.abs(enemy.y - wreckY) < 50) {
	    		detectedEnemyWreck = enemy;
	    		break;
	    	}
	    	
	    }
	    enemyTargets.remove(detectedEnemyWreck);
	    enemyPosToAvoid.add(detectedEnemyWreck);    
	}
    
    private void handleEnemyMessage(String[] parts) {
    	//sendLogMessage("handleEnemyMessage");
    	fireOrder = true;
        double enemyX = Double.parseDouble(parts[4]);
        double enemyY = Double.parseDouble(parts[5]);
        double enemyDistance = Double.parseDouble(parts[2]);
        double enemyDirection = Double.parseDouble(parts[1]);
        Types enemyType = parts[3].contains("MainBot") ? Types.OpponentMainBot : Types.OpponentSecondaryBot;

        addOrUpdateEnemy(enemyX, enemyY, enemyDistance, enemyDirection, false, enemyType);
    }
    
    private void handleFire(Ennemy target) {
        if (target != null) {
            if (avoidingEnnemy) {
        		if (!isRoughlySameDirection(target.direction, getHeading())) {
                	turnTo(target.direction);
                	return;
        		} 
        		if (distance(new Position(target.x, target.y), myPos) < 800) {
        			myMove(false);
        		} else {
        			myMove(true);
        		}
            	avoidingEnnemy = false;
                //moveTowardsTarget(target);
                return; 
            }
            firePosition(target.x, target.y);
        }  else {
            state = State.MOVING;
            sendLogMessage("state moving dans handleFire");
            fireOrder = false;
        }
    }
	
	private void firePosition(double x, double y) {
	    double angle = Math.atan2(y - myPos.getY(), x - myPos.getX());
	    fire(angle);
	    if (target.equals(lastTarget)) fireStrike++;
	    else {
	    	fireStrike = 0;
	    }
	    if(fireStrike >= MAX_FIRE_STREAK) {
	    	enemyTargets.remove(target);
	    	fireStrike = 0;
	    }
        avoidingEnnemy = true;
	    System.out.println("fireINNNNNG " + whoAmI);
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
	        	//sendLogMessage("ennemy updated");
	        	enemy.updatePosition(x, y, distance, direction);
	            return;
	        }
	    }
	    //sendLogMessage("ennemy added");
	    enemyTargets.add(new Ennemy(x, y, distance, direction, type));
	    
	}
	private boolean isObstacleOnMyFire(Position obstacleCenter, Position target, double obstacleRadius) {
	    double startX = myPos.getX();
	    double startY = myPos.getY();
	    double endX = target.getX();
	    double endY = target.getY();
	    double allyX = obstacleCenter.getX();
	    double allyY = obstacleCenter.getY();

	    // Vector from shooter to target
	    double deltaX = endX - startX;
	    double deltaY = endY - startY;
	    double lineLength = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

	    // If shooter and target are the same point, no firing line exists
	    if (lineLength == 0) {
	        return false;
	    }

	    // Normalized direction vector
	    double dirX = deltaX / lineLength;
	    double dirY = deltaY / lineLength;

	    // Vector from shooter to ally
	    double vecX = allyX - startX;
	    double vecY = allyY - startY;

	    // Projection of ally onto the firing line
	    double projection = vecX * dirX + vecY * dirY;

	    // If ally is behind shooter or beyond target, it’s not in the way
	    if (projection < 0 || projection > lineLength) {
	        return false;
	    }

	    // Perpendicular distance from ally to the firing line (cross product magnitude)
	    double perpendicularDistance = Math.abs(vecX * dirY - vecY * dirX);

	    // Effective radius includes bullet size and ally size
	    double effectiveRadius = obstacleRadius + Parameters.bulletRadius;

	    // If ally is too close to the firing line, it’s an obstacle
	    return perpendicularDistance < effectiveRadius;
	}
	
	private Ennemy chooseTarget() {
	    Collections.sort(enemyTargets, (e1, e2) -> Double.compare(e1.distance, e2.distance));
	    for (Ennemy enemy : enemyTargets) {
	        boolean obstacleInTheWay = false;
	        Position target = new Position(enemy.x, enemy.y);
	        
	        // Check each ally
	        for (BotState ally : allyPos.values()) {
	            Position allyCenter = ally.getPosition();
	            if (allyCenter.getX() == myPos.getX() && allyCenter.getY() == myPos.getY()) {
	                continue; 
	            }
	            // Use BOT_RADIUS as the ally's size
	            if (isObstacleOnMyFire(allyCenter, target, BOT_RADIUS)) {
	            	//System.out.println("ally on the wayyyyyyyyyyyyyyy");
	            	obstacleInTheWay = true;
	                break;
	            }
	        }
	        for (double[] wreck : wreckPositions) {
	            Position wreckCenter = new Position(wreck[0], wreck[1]);
	            if (isObstacleOnMyFire(wreckCenter, target, BOT_RADIUS)) {
	            	//System.out.println("wreeck on the wayyyyyyyyyyyyyyy");
	            	obstacleInTheWay = true;
	                break;
	            }
	        }

	        if (!obstacleInTheWay) {
	            return enemy; // Safe to fire at this enemy
	        }
	    }
	    return null; // No safe target found
	}

	
	private void reach_ennemy(double tX, double tY) {
	    double angleToTarget = Math.atan2(tY - myPos.getY(), tX - myPos.getX());

	    // Calculer la distance à l'ennemi
	    double distanceToScout = Math.sqrt(Math.pow(myPos.getX() - tX, 2) + Math.pow(myPos.getY() - tY, 2));


	    if (distanceToScout > 950) {
	        // Trop loin : s'approcher de l'ennemi
	        angleToTarget = getNearestAllowedDirection(angleToTarget);

	        if (!isSameDirection(getHeading(), angleToTarget)) {
	            turnTo(angleToTarget);
	        } else {
	            myMove(true); // Avancer vers l'ennemi
	        }
	    } else {
	        // Trop près : reculer de l'ennemi
	        double angleToMoveBack = angleToTarget + Math.PI; // Inverser la direction (180°)
	        angleToMoveBack = getNearestAllowedDirection(angleToMoveBack);

	        if (!isSameDirection(getHeading(), angleToMoveBack)) {
	            turnTo(angleToMoveBack);
	        } else {
	            myMove(false); // Reculer
	        }
	    }
	}
	
}