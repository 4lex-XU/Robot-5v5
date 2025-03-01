package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

abstract class MacDuoBaseBot extends Brain {
	
}

public class SecondaryMacDuo extends MacDuoBaseBot{

	private static final double ANGLEPRECISION = 0.1;
    private static final String NBOT = "NBOT";
    private static final String SBOT = "SBOT";	
    
    private enum State { RDV_POINT, WAITING, MOVING, MOVING_BACK, TURNING_LEFT, TURNING_RIGHT };

    // points de rdv
    private static final double TARGET_X1 = 600; // TargetX2 - 300
    private static final double TARGET_Y1 = 840; // TargetY2 - 660
    
    private static final double TARGET_X2 = 900; 
    private static final double TARGET_Y2 = 1500;
    
    //---VARIABLES---//
    private State state;
	private String whoAmI;
	private double myX,myY;
	private boolean isMoving;
	private double oldAngle;
	
	private Map<String, Double[]> allyPos = new HashMap<>();	// Stocker la position des alliés
	private Map<String, Double[]> wreckPos = new HashMap<>();	// Stocker la position des débris
	
	//=========================================CORE=========================================	
	public SecondaryMacDuo() {super();}

	@Override
	public void activate() {
		//détermination de l'emplacement du bot
		whoAmI = NBOT;
		for (IRadarResult o: detectRadar()) {
			if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI = SBOT;
		}
		if (whoAmI == NBOT){
			myX = (isHeading(Parameters.EAST)) ? Parameters.teamASecondaryBot1InitX : Parameters.teamBSecondaryBot1InitX;
		    myY = (isHeading(Parameters.EAST)) ? Parameters.teamASecondaryBot1InitY : Parameters.teamBSecondaryBot1InitY;
	    } else {
	    	myX = (isHeading(Parameters.EAST)) ? Parameters.teamASecondaryBot2InitX : Parameters.teamBSecondaryBot2InitX;
	    	myY = (isHeading(Parameters.EAST)) ? Parameters.teamASecondaryBot2InitY : Parameters.teamBSecondaryBot2InitY;
	    }

	    sendLogMessage(whoAmI+" activé, position : " + myX + "," + myY);
		
	    //INIT
	    isMoving=true;
	    state = State.RDV_POINT;
	    oldAngle = getHeading();
	}

	@Override
	public void step() {
		detectAndBroadCast();
		
		switch (state) {
			case RDV_POINT :
				double tX = (whoAmI == SBOT) ? TARGET_X2 : TARGET_X1;
			    double tY = (whoAmI == SBOT) ? TARGET_Y2 : TARGET_Y1;
				moveToTarget(tX, tY);
				break;
			case WAITING :
				readMessages();
				break;
			case MOVING :
				readMessages();
				myMove();
				break;
			case MOVING_BACK :
				moveBack();
				break;
			case TURNING_LEFT :
				myTurningTask();
				break;
			case TURNING_RIGHT:
				break;
		}
	  
	}
	
	//=========================================BASE=========================================
	
		protected boolean isSameDirection(double dir1, double dir2) {
		    double diff = normalize(dir1 - dir2);
		    if (diff > Math.PI) diff -= 2 * Math.PI;
		    if (diff < -Math.PI) diff += 2 * Math.PI;
		    return Math.abs(diff) < ANGLEPRECISION;
		}
		
		protected double normalize(double dir){
		    double res=dir;
		    while (res<0) res+=2*Math.PI;
		    while (res>=2*Math.PI) res-=2*Math.PI;
		    return res;
		}
		
		protected boolean isHeading(double dir){
			return Math.abs(Math.sin(getHeading()-dir))<ANGLEPRECISION;
		}
	
	//=========================================ADDED=========================================
	
	// se déplace au point de rdv donné 
	private void moveToTarget(double tX, double tY) {
	    double dx = tX - myX;
	    double dy = tY - myY;
	    double angleToTarget = Math.atan2(dy, dx);

	    if (!isSameDirection(getHeading(), angleToTarget)) {
	        turnTo(angleToTarget);
	        sendLogMessage("Rotation vers la cible...");
	    } else {
	        sendLogMessage("Angle correct, déplacement...");
	        myMove();	       
	    }
	    if (Math.abs(myX - tX) < 5 && Math.abs(myY - tY) < 5) {
	        sendLogMessage("Position cible atteinte !");
	        state = state.WAITING;
	    }
	}
	
	private void myMove() {
		switch (detectFront().getObjectType()) {
			case NOTHING :
				move(); 
	            myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
	            myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
	            break;
	      
	        default:
	            state = State.TURNING_LEFT;
	            oldAngle = getHeading();
	            break;
	    }        
		broadcast("POS "+whoAmI+ " " +myX+" "+myY);
	}
	
	
	private void detectAndBroadCast() {
		// Détection des ennemis et envoi d'infos
	    for (IRadarResult o : detectRadar()) {
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || 
	            o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            
	            // Transmettre la position des ennemis : ENEMY dir dist type enemyX enemyY
	            double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
	            double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
	            String message = "ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY;
	            broadcast(message);
	            sendLogMessage(message);
	        }
	    }
	}

	// En state TURNING
	// Tourne à gauche
	private void myTurningTask() {
		    if (!isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
		            stepTurn(Parameters.Direction.LEFT);
		    } else {
		    	System.out.println("trying to move");
		        state = State.MOVING;
		        myMove();
		    }
	}
	/*
	private void myMove() {
    IFrontSensorResult frontObject = detectFront();
    
    switch (frontObject.getObjectType()) {
        case WALL:
            moveBack();
            myX -= Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY -= Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
            turnTo(getHeading() + Math.PI / 2); // Contourne en tournant 90°
            break;
            
        case OpponentMainBot:
        case OpponentSecondaryBot:
            moveBack();
            myX -= Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY -= Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
            turnTo(getHeading() + Math.PI / 4); // Évite en tournant de 45°
            break;
            
        case BULLET:
            turnTo(getHeading() + Math.PI / 2); // Tourne à 90° pour esquiver
            myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
            break;
            
        case Wreck:
            turnTo(getHeading() + Math.PI / 3); // Tourne de 60° pour contourner
            myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
            break;

        case TeamMainBot:
        case TeamSecondaryBot:
            turnTo(getHeading() + Math.PI / 6); // Ajuste légèrement la direction
            myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
            break;
            
        default:
            move(); // Avance normalement
            myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
            break;
    }

    // 📡 Mise à jour et transmission de la position
    broadcast("POS " + whoAmI + " " + myX + " " + myY);
}
	 */
	
	// StepTurn Gauche ou Droite, selon un angle cible
	protected void turnTo(double targetAngle) {
	    double currentAngle = getHeading();
	    double diff = normalize(targetAngle - currentAngle);
	    if (diff > Math.PI) {
	        diff -= 2 * Math.PI;  
	    } else if (diff < -Math.PI) {
	        diff += 2 * Math.PI; 
	    }
	    if (diff > ANGLEPRECISION) {
	        stepTurn(Parameters.Direction.RIGHT);
	    } else if (diff < -ANGLEPRECISION) {
	        stepTurn(Parameters.Direction.LEFT);
	    }
	}
	
	private void sendPosition() {
		broadcast("POS "+whoAmI+" "+myX+" "+myY);
	}
	
	// Interprète les messages des alliés
	private void readMessages() {
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
	            case "POS" :
	        	    state = state.MOVING;
	            	double targetX = Double.parseDouble(parts[2]);
	                double targetY = Double.parseDouble(parts[3]);
	            	allyPos.put(parts[1], new Double[]{targetX, targetY});
	            	double distance = Math.sqrt(Math.pow(targetX - myX, 2) + Math.pow(targetY - myY, 2));
	        	    if (distance > 800 && parts[1] != "NBOT" && parts[1] != "SBOT") {
	        	        state = state.WAITING;
	        	    }
	            	break;
                
            }
        }
    }
	
	
}
