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
	    }
	    
	    public void updatePosition(double newX, double newY, double newDistance, double newDirection) {
	        this.previousX = this.x;
	        this.previousY = this.y;
	        this.previousDirection = this.direction;

	        this.x = newX;
	        this.y = newY;
	        this.distance = newDistance;
	        this.direction = newDirection;
	        this.speed = (x - previousX);
	    }
	    
		public double getSpeed() {
//	    	double speed = 0;
//	    	switch (type) {
//	    		case OpponentMainBot :
//	    			speed = Parameters.teamBMainBotSpeed;
//	    			break;
//	    		case OpponentSecondaryBot :	
//	    			speed = Parameters.teamBSecondaryBotSpeed;
//	    			break;
//	    	}
			return speed;
	    }
	    public double predictX(double bulletTravelTime) {
	        double actualDirection = Math.atan2(y - previousY, x - previousX);
	        return x + Math.cos(actualDirection) * speed * bulletTravelTime;
	    }

//	    public double predictX(double bulletTravelTime) {
//	        return x + Math.cos(direction) * getSpeed() * bulletTravelTime;
//	    }
	    public double predictY(double bulletTravelTime) {
	        double actualDirection = Math.atan2(y - previousY, x - previousX);
	        return y + Math.sin(actualDirection) * speed * bulletTravelTime;
	    }
//	    public double predictY(double bulletTravelTime) {
//	        return y + Math.sin(direction) * getSpeed() * bulletTravelTime;
//	    }
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
    private double rdvX, rdvY; 
    private boolean fireOrder;
    private Parameters.Direction turnedDirection;
    
    private boolean obstacleDetected = false;
    private boolean obstacleInWay = false;
    private double obstacleDirection = 0;
    private int avoidanceTimer = 0;
    private static final int AVOIDANCE_DURATION = 10;

    private boolean following;
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
	    state = State.FIRST_RDV;
	    rdv_point = true;
	    oldAngle = myGetHeading();
		following = true;
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
		if (state == State.FIRE && !isShooterAvoiding) {
			handleFire();
		}
		readMessages();
		
		if (getHealth() <= 0) {
			state = State.DEAD;
			allyPos.put(whoAmI, new BotState(myPos.getX(), myPos.getY(), false));
			return;
		}
		
		try {
			switch (state) {
				case FIRST_RDV:
					if (rdv_point) {
						reach_rdv_point(rdvX, rdvY);
					}
					break;
				case MOVING:
				    if ((following || allyPos.get(NBOT).isAlive()) && !isShooterAvoiding) {
				        reach_rdv_point(rdvX, rdvY);
				    } else {
				        // En mode évitement, on avance
				        if (!hasReachedTarget(targetX, targetY, true)) {
				            myMove(true);
				        } else {
				            // La cible d'évitement est atteinte
				            isShooterAvoiding = false;
				        }
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
				            sendLogMessage("Obstacle détecté dans la trajectoire circulaire !");
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
	        
//	        // Obstacle detection for movement
//			obstacleDirection = o.getObjectDirection();
//	        if ((o.getObjectDistance() <= OBSTACLE_AVOIDANCE_DISTANCE && ((isSameDirection(obstacleDirection, Parameters.NORTH) || isSameDirection(obstacleDirection, Parameters.SOUTH)))) 
//					|| detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING ) {
//				obstacleDetected = true;
//	        	initiateObstacleAvoidance();
//				//System.out.println("Obstacle detected at direction: " + (obstacleDirection * 180 / Math.PI) + "°");
//	            //sendLogMessage("Obstacle detected at direction: " + (obstacleDirection * 180 / Math.PI) + "°");
//	        }
	    }
	    
	    if (enemyDetected && !isShooterAvoiding) {
	        state = State.FIRE;
	        //sendLogMessage("state fire dans detection");
	        avoidanceTimer = 0;
	    } else if (!enemyDetected && state != State.FIRE && !isShooterAvoiding){
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
	           /* case "MOVING_BACK":
	            	double enemyX = Double.parseDouble(parts[2]);
	                double enemyY = Double.parseDouble(parts[3]);
	            	double distance = Math.sqrt(Math.pow(enemyX - myPos.getX(), 2) + Math.pow(enemyY - myPos.getY(), 2));
	            	if (distance < 700){
	            		state = State.MOVING_BACK;
	        	        //sendLogMessage("state moving back dans readMessages");
	            		myMove(false);
	            	}
	            	break;*/
				case "DEAD":
					allyPos.put(parts[1], new BotState( allyPos.get(parts[1]).getPosition().getX(), 
														allyPos.get(parts[1]).getPosition().getY(), 
														false));
					if (parts[1].equals(SBOT)) {
						following = false;
					}
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
		if (!isShooterAvoiding) handleFire(); 
    }
    
    private void handleFire() {
        Ennemy target = chooseTarget();
        if (target != null) {
            if (target.distance > 990) {
                //moveTowardsTarget(target);
            	state = State.MOVING;
                return; 
            }
			// passe en mode fire si un ennemi est à porter
			state = State.FIRE;

            if (lastTarget != null && lastTarget.equals(target)) {
                fireStreak++;
                if (fireStreak >= MAX_FIRE_STREAK) {
                    //sendLogMessage("Target stuck for too long, switching target.");
                    enemyTargets.remove(target);
                    fireStreak = 0;
                    lastTarget = null;

                    target = chooseTarget();
                    if (target == null) {
                        //sendLogMessage("No more targets, switching to MOVING.");
                        state = State.MOVING; 
                        fireOrder = false;
                        return;
                    } else {
                        lastTarget = target;
                    } 
                }
            } else {
                fireStreak = 0; 
                lastTarget = target;
            }

            if (!enemyTargets.contains(target)) {
                //sendLogMessage("Target eliminated, stopping fire.");
                fireOrder = false;
                state = State.MOVING;
                return;
            }
            
            if (obstacleInWay) {
                repositionForShooting(target);
                return;
            }
            
            if (fireOrder) {
//            	if (target.getSpeed() > 1) {
//            	    firePositionWithPrediction(target);
//            	} else {
            	    firePosition(target.x, target.y);
            	//}
            }
        }  else {
            state = State.MOVING;
            //sendLogMessage("state moving dans handleFire");
            fireOrder = false;
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
	    //sendLogMessage("Firing at predicted position: (" + (int) predictedX + ", " + (int) predictedY + ")");
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
	        	//sendLogMessage("ennemy updated");
	        	enemy.updatePosition(x, y, distance, direction);
	            return;
	        }
	    }
	    //sendLogMessage("ennemy added");
	    enemyTargets.add(new Ennemy(x, y, distance, direction, type));
	}
	
	private Ennemy chooseTarget() {
	    Collections.sort(enemyTargets, (e1, e2) -> Double.compare(e1.distance, e2.distance));
	    //sendLogMessage("choosetarget target len : " + enemyTargets.size());
	    // Parcours les ennemis et choisit celui qui est le plus proche sans allié sur la trajectoire
	    for (Ennemy ennemy : enemyTargets) {
	        boolean allyInTheWay = false;
	        for (BotState ally : allyPos.values()) { 

	            double allyX = ally.getPosition().getX();
	            double allyY = ally.getPosition().getY();
	            if (allyX == myPos.getX() && allyY == myPos.getY()) {
	                continue;
	            }

	            if (isObstacleOnMyFire(getObstacleCorners(BOT_RADIUS, allyX, allyY), new Position(ennemy.x, ennemy.y))) {
	            	allyInTheWay = true;
	                //sendLogMessage("(" + allyX + ", " + allyY + ") aligned with enemy at (" + enemy.x + ", " + enemy.y + ")");                  
	                break;
	            }
	        }
	        if (!allyInTheWay) {
	        	//sendLogMessage("ennemy");
	            return ennemy;
	        }
	    }
	    //sendLogMessage("ennemy not found " + enemyTargets.size());
	    return null;
	}
	
	private boolean isObstacleOnMyFire(Position[] obstacleCorners, Position target) {
		boolean isObstructed = false;
		Position[][] trajectoryEdges = getBulletTrajectoryEdges(myPos, target, Parameters.bulletRadius);
        Segment[] obstacleEdges = getObstacleEdges(obstacleCorners);
        for (Position[] edge : trajectoryEdges) {
            Position tStart = edge[0];
            Position tEnd = edge[1];
            for (Segment obsEdge : obstacleEdges) {
                if (segmentsIntersect(tStart, tEnd, obsEdge.start, obsEdge.end)) {
                    isObstructed = true;
                    break;
                }
            }
            if (isObstructed) break;

        }
        return isObstructed;
	}
	
	private boolean segmentsIntersect(Position p1, Position q1, Position p2, Position q2) {
	    int o1 = orientation(p1, q1, p2);
	    int o2 = orientation(p1, q1, q2);
	    int o3 = orientation(p2, q2, p1);
	    int o4 = orientation(p2, q2, q1);
	    
	    // Cas général : les orientations sont différentes.
	    if (o1 != o2 && o3 != o4)
	        return true;
	    
	    // Cas particuliers : p1, q1 et p2 sont colinéaires et p2 se trouve sur le segment p1-q1
	    if (o1 == 0 && onSegment(p1, p2, q1)) return true;
	    // p1, q1 et q2 sont colinéaires et q2 se trouve sur le segment p1-q1
	    if (o2 == 0 && onSegment(p1, q2, q1)) return true;
	    // p2, q2 et p1 sont colinéaires et p1 se trouve sur le segment p2-q2
	    if (o3 == 0 && onSegment(p2, p1, q2)) return true;
	    // p2, q2 et q1 sont colinéaires et q1 se trouve sur le segment p2-q2
	    if (o4 == 0 && onSegment(p2, q1, q2)) return true;
	    
	    return false;
	}
	
	private int orientation(Position p, Position q, Position r) {
	    double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) - 
	                 (q.getX() - p.getX()) * (r.getY() - q.getY());
	    if (Math.abs(val) < 1e-9) return 0;  // colinéaire
	    return (val > 0) ? 1 : 2;  // 1 = horaire, 2 = antihoraire
	}
	
	private boolean onSegment(Position p, Position q, Position r) {
	    return q.getX() <= Math.max(p.getX(), r.getX()) &&
	           q.getX() >= Math.min(p.getX(), r.getX()) &&
	           q.getY() <= Math.max(p.getY(), r.getY()) &&
	           q.getY() >= Math.min(p.getY(), r.getY());
	}


	protected Segment[] getObstacleEdges(Position[] corners) {
	    // Ici, on définit un segment par paire de coins consécutifs.
	    Segment edge1 = new Segment(corners[0], corners[1]);
	    Segment edge2 = new Segment(corners[1], corners[2]);
	    Segment edge3 = new Segment(corners[2], corners[3]);
	    Segment edge4 = new Segment(corners[3], corners[0]);
	    return new Segment[] { edge1, edge2, edge3, edge4 };
	}

	protected Position[][] getBulletTrajectoryEdges(Position shooter, Position target, double bulletRadius) {
	    double dx = target.getX() - shooter.getX();
	    double dy = target.getY() - shooter.getY();
	    double length = Math.sqrt(dx * dx + dy * dy);
	    if (length == 0) return null; // Cas particulier

	    double ndx = dx / length;
	    double ndy = dy / length;

	    // Vecteur perpendiculaire (rotation de 90°)
	    double nx = -ndy;
	    double ny = ndx;

	    Position shooter1 = new Position(shooter.getX() + bulletRadius * nx, shooter.getY() + bulletRadius * ny);
	    Position target1  = new Position(target.getX() + bulletRadius * nx, target.getY() + bulletRadius * ny);
	    Position shooter2 = new Position(shooter.getX() - bulletRadius * nx, shooter.getY() - bulletRadius * ny);
	    Position target2  = new Position(target.getX() - bulletRadius * nx, target.getY() - bulletRadius * ny);

	    return new Position[][] { { shooter1, target1 }, { shooter2, target2 } };
	}

	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private double perpendicularDistance(double allyX, double allyY, double x1, double y1, double x2, double y2) {
	    double numerator = Math.abs((y2 - y1) * allyX - (x2 - x1) * allyY + x2 * y1 - y2 * x1);
	    double denominator = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
	    return numerator / denominator;
	}
	
	private void moveTowardsTarget(Ennemy target) {
	    //sendLogMessage("moveTowardsTarget | Current Pos: (" + myPos.getX() + ", " + myPos.getY() + ") | Target Pos: (" + target.x + ", " + target.y + ")");

	    double angleToTarget = Math.atan2(target.y - myPos.getY(), target.x - myPos.getX());
	    double currentHeading = getHeading();

	    if (!isRoughlySameDirection(currentHeading, angleToTarget)) {
	        //sendLogMessage("Turning towards target | Current Heading: " + Math.toDegrees(currentHeading) + "° | Target Angle: " + Math.toDegrees(angleToTarget) + "°");
	        turnTo(angleToTarget);  
	        return;
	    }
	    sendLogMessage("Aligned with target. Moving forward...");
	    myMove(true);
	    state = State.MOVING;
	}
	
//	private Ennemy chooseTarget() {
//	    Collections.sort(enemyTargets, (e1, e2) -> Double.compare(e1.distance, e2.distance));
//
//	    for (Ennemy enemy : enemyTargets) {
//	        obstacleInWay = false;
//
//	        // Vérifie si un allié ou un wreck est entre le shooter et l'ennemi
//	        for (BotState ally : allyPos.values()) {
//	        	
//	            double allyX = ally.getPosition().getX();
//	            double allyY = ally.getPosition().getY();
//	            if (allyX == myPos.getX() && allyY == myPos.getY()) {
//	                continue;
//	            }
//	            if (isObstacleOnLine(myPos.getX(), myPos.getY(), enemy.x, enemy.y, allyX, allyY)) {
//	                obstacleInWay = true;
//	                sendLogMessage("Ally blocking shot at: (" + allyX + ", " + allyY + ")");
//	                break;
//	            }
//	        }
//
//	        for (double[] wreck : wreckPositions) {
//	            double wreckX = wreck[0];
//	            double wreckY = wreck[1];
//	            if (isObstacleOnLine(myPos.getX(), myPos.getY(), enemy.x, enemy.y, wreckX, wreckY)) {
//	                obstacleInWay = true;
//	                sendLogMessage("Wreck blocking shot at: (" + wreckX + ", " + wreckY + ")");
//	                break;
//	            }
//	        }
//
//	        if (!obstacleInWay) {
//	            return enemy; 
//	        }
//	    }
//	    return null;
//	}
	private boolean isObstacleOnLine(double x1, double y1, double x2, double y2, double ox, double oy) {
	    // Calcul du produit vectoriel pour trouver la distance du point à la ligne
	    double crossProduct = Math.abs((oy - y1) * (x2 - x1) - (ox - x1) * (y2 - y1));
	    double lineLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	    double distanceToLine = crossProduct / lineLength;
	    
	    // Rayon estimé des obstacles (ajustez selon votre environnement)
	    double obstacleRadius = 30;
	    
	    // Vérifier si le point est entre les extrémités de la ligne et à une distance inférieure au rayon
	    if (distanceToLine < obstacleRadius) {
	        // Vérifier si le point est "entre" les extrémités de la ligne
	        // Calculer la projection du point sur la ligne
	        double dotProduct = ((ox - x1) * (x2 - x1) + (oy - y1) * (y2 - y1)) / (lineLength * lineLength);
	        
	        // Si la projection est entre 0 et 1, le point est entre les extrémités
	        return dotProduct >= 0 && dotProduct <= 1;
	    }
	    
	    return false;
	}
	
	private void repositionForShooting(Ennemy target) {
	    double angleToTarget = Math.atan2(target.y - myPos.getY(), target.x - myPos.getX());
	    
	    // Identifier l'obstacle qui bloque le tir
	    double obstacleX = -1;
	    double obstacleY = -1;
	    
	    // Vérifier si un allié bloque
	    for (BotState ally : allyPos.values()) {
	        double allyX = ally.getPosition().getX();
	        double allyY = ally.getPosition().getY();
	        
	        if (allyX == myPos.getX() && allyY == myPos.getY()) {
	            continue;
	        }
	        
	        if (isObstacleOnLine(myPos.getX(), myPos.getY(), target.x, target.y, allyX, allyY)) {
	            obstacleX = allyX;
	            obstacleY = allyY;
	            break;
	        }
	    }
	    
	    // Vérifier si une épave bloque
	    if (obstacleX == -1) {
	        for (double[] wreck : wreckPositions) {
	            double wreckX = wreck[0];
	            double wreckY = wreck[1];
	            
	            if (isObstacleOnLine(myPos.getX(), myPos.getY(), target.x, target.y, wreckX, wreckY)) {
	                obstacleX = wreckX;
	                obstacleY = wreckY;
	                break;
	            }
	        }
	    }
	    
	    // Si on a identifié un obstacle
	    if (obstacleX != -1) {
	        // Calculer le vecteur robot-obstacle
	        double toObstacleX = obstacleX - myPos.getX();
	        double toObstacleY = obstacleY - myPos.getY();
	        
	        // Normaliser ce vecteur
	        double distance = Math.sqrt(toObstacleX * toObstacleX + toObstacleY * toObstacleY);
	        toObstacleX /= distance;
	        toObstacleY /= distance;
	        
	        // Créer un vecteur perpendiculaire (deux options possibles)
	        double perpX1 = -toObstacleY;
	        double perpY1 = toObstacleX;
	        double perpX2 = toObstacleY;
	        double perpY2 = -toObstacleX;
	        
	        // Calculer les deux positions potentielles (à gauche et à droite de l'obstacle)
	        double moveDistance = 60; // Distance de déplacement
	        double posX1 = myPos.getX() + perpX1 * moveDistance;
	        double posY1 = myPos.getY() + perpY1 * moveDistance;
	        double posX2 = myPos.getX() + perpX2 * moveDistance;
	        double posY2 = myPos.getY() + perpY2 * moveDistance;
	        
	        // Déterminer quelle position offre le meilleur angle vers la cible
	        double angle1ToTarget = Math.atan2(target.y - posY1, target.x - posX1);
	        double angle2ToTarget = Math.atan2(target.y - posY2, target.x - posX2);
	        
	        // Vérifier quelle position donne une ligne de tir claire
	        boolean pos1Clear = !isPositionBlocked(posX1, posY1, target.x, target.y);
	        boolean pos2Clear = !isPositionBlocked(posX2, posY2, target.x, target.y);
	        
	        double bestPerpX, bestPerpY;
	        double bestAngle;
	        
	        if (pos1Clear && pos2Clear) {
	            // Les deux positions sont claires, choisir celle qui est la plus proche de notre direction actuelle
	            double currentAngle = getHeading();
	            double diff1 = Math.abs(normalize(angle1ToTarget - currentAngle));
	            double diff2 = Math.abs(normalize(angle2ToTarget - currentAngle));
	            
	            if (diff1 < diff2) {
	                bestPerpX = perpX1;
	                bestPerpY = perpY1;
	                bestAngle = Math.atan2(perpY1, perpX1);
	            } else {
	                bestPerpX = perpX2;
	                bestPerpY = perpY2;
	                bestAngle = Math.atan2(perpY2, perpX2);
	            }
	        } else if (pos1Clear) {
	            bestPerpX = perpX1;
	            bestPerpY = perpY1;
	            bestAngle = Math.atan2(perpY1, perpX1);
	        } else if (pos2Clear) {
	            bestPerpX = perpX2;
	            bestPerpY = perpY2;
	            bestAngle = Math.atan2(perpY2, perpX2);
	        } else {
	            // Aucune position ne donne une ligne de tir claire
	            // Se déplacer simplement perpendiculairement à l'obstacle
	            bestPerpX = perpX1; // Choix arbitraire
	            bestPerpY = perpY1;
	            bestAngle = Math.atan2(perpY1, perpX1);
	        }
	        
	        // Se tourner et se déplacer vers la position choisie
	        turnTo(bestAngle);
	        myMove(true);
	        
	        sendLogMessage("Repositioning perpendicular to obstacle for clear shot");
	    } else {
	        // Pas d'obstacle identifié (cas anormal)
	        sendLogMessage("No obstacle identified but shot is blocked. Moving randomly.");
	        turnTo(angleToTarget + Math.PI/2);
	        myMove(true);
	    }
	}

	// Fonction auxiliaire pour vérifier si une position donne une ligne de tir claire
	private boolean isPositionBlocked(double fromX, double fromY, double toX, double toY) {
	    // Vérifier les alliés
	    for (BotState ally : allyPos.values()) {
	        double allyX = ally.getPosition().getX();
	        double allyY = ally.getPosition().getY();
	        
	        if (allyX == myPos.getX() && allyY == myPos.getY()) {
	            continue;
	        }
	        
	        if (isObstacleOnLine(fromX, fromY, toX, toY, allyX, allyY)) {
	            return true;
	        }
	    }
	    
	    // Vérifier les épaves
	    for (double[] wreck : wreckPositions) {
	        double wreckX = wreck[0];
	        double wreckY = wreck[1];
	        
	        if (isObstacleOnLine(fromX, fromY, toX, toY, wreckX, wreckY)) {
	            return true;
	        }
	    }
	    
	    return false;
	}
}