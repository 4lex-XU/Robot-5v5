package algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import algorithms.MacDuoBaseBot.State;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

//====================================================================================
//====================================ABSTRACT BOT====================================
//====================================================================================

abstract class MacDuoBaseBot extends Brain {
	
	protected static final double ANGLEPRECISION = 0.1;
	protected enum State { MOVING, MOVING_BACK, TURNING_LEFT, TURNING_RIGHT, FIRE };
	
	protected final double BOT_RADIUS = 50;
	protected final double BULLET_RADIUS = 5;

	protected String whoAmI;
	protected double myX, myY;
	protected boolean freeze;
    protected boolean isTeamA;
	protected boolean rdv_point;
	protected boolean turningTask = false;
	
	 //---VARIABLES---//
	protected State state;
	protected boolean isMoving;
	protected double oldAngle;
	
	protected Map<String, Double[]> allyPos = new HashMap<>();	// Stocker la position des alliés
	protected Map<String, Double[]> wreckPos = new HashMap<>();	// Stocker la position des débris
	
	protected abstract void myMove();
	protected abstract void detection();
	
	// se déplace au point de rdv donné 
	protected void reach_rdv_point(double tX, double tY) {
		if (rdv_point) {
		    double angleToTarget = Math.atan2(tY - myY, tX - myX);
	
		    if (!isSameDirection(getHeading(), angleToTarget)) {
		        turnTo(angleToTarget);
		        //sendLogMessage("Rotation vers la cible...");

		    } else {
		        //sendLogMessage("Angle correct, déplacement...");
		        myMove();	       
		    }
		    if (Math.abs(myX - tX) < 5 && Math.abs(myY - tY) < 5) {
		    	rdv_point = false;
		        //sendLogMessage("Position cible atteinte !");
		    }
		    freeze = true;
		}
	}
	
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
	
	//=========================================BASE=========================================

	protected boolean isSameDirection(double dir1, double dir2) {
	    return Math.abs(normalize(dir1)-normalize(dir2))<ANGLEPRECISION;
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
	
	protected double myGetHeading(){
	    return normalize(getHeading());
	  }
	
	protected void sendMyPosition() {
		broadcast("POS "+whoAmI+" "+myX+" "+myY);
	}
	
	protected void turnLeft() {
		
		if (!isSameDirection(getHeading(),oldAngle+Parameters.LEFTTURNFULLANGLE)) {
			stepTurn(Parameters.Direction.LEFT);
	    } else {
	    	turningTask = false;
	    	System.out.println("trying to move");
	        state = State.MOVING;
	        myMove();
	    }				
	}
	
	protected void turnRight() {
		if (!isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
            stepTurn(Parameters.Direction.RIGHT);
	    } else {
	    	turningTask = false;
	    	System.out.println("trying to move");
	        state = State.MOVING;
	        myMove();
	    }				
	}
}

//=================================================================================
//====================================SECONDARY====================================
//=================================================================================

public class SecondaryMacDuo extends MacDuoBaseBot{

    private static final String NBOT = "NBOT";
    private static final String SBOT = "SBOT";	
    
    private int count;
    
    // points de rdv
    private static final double TARGET_X1 = 600; // TargetX2 - 300
    private static final double TARGET_Y1 = 840; // TargetY2 - 660
    private static final double TARGET_X2 = 900; 
    private static final double TARGET_Y2 = 1500;
    private double targetX, targetY; 
	
	//=========================================CORE=========================================	
	public SecondaryMacDuo() {super();}

	@Override
	public void activate() {
		isTeamA = isHeading(Parameters.EAST);

		//détermination de l'emplacement du bot
		whoAmI = NBOT;
		for (IRadarResult o: detectRadar()) {
			if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI = SBOT;
		}
		if (whoAmI == NBOT){
			myX = (isTeamA) ? Parameters.teamASecondaryBot1InitX : Parameters.teamBSecondaryBot1InitX;
		    myY = (isTeamA) ? Parameters.teamASecondaryBot1InitY : Parameters.teamBSecondaryBot1InitY;
	    } else {
	    	myX = (isTeamA) ? Parameters.teamASecondaryBot2InitX : Parameters.teamBSecondaryBot2InitX;
	    	myY = (isTeamA) ? Parameters.teamASecondaryBot2InitY : Parameters.teamBSecondaryBot2InitY;
	    }

	    sendLogMessage(whoAmI+" activé, position : " + myX + "," + myY);
		
	    //INIT
	    isMoving=true;
	    rdv_point=true;
	    state = State.MOVING;
	    oldAngle = getHeading();
	    targetX = (whoAmI == SBOT) ? TARGET_X2 : TARGET_X1;
	    targetY = (whoAmI == SBOT) ? TARGET_Y2 : TARGET_Y1;
	}

	@Override
	public void step() {
		
		
		detection();
		readMessages();
		reach_rdv_point(targetX, targetY);
		
		if (freeze) return;
		
		switch (state) {
			case MOVING :
				myMove();
				break;
			case MOVING_BACK :
				moveBack();
				break;
			case TURNING_LEFT :
				turnLeft();
				break;
			case TURNING_RIGHT:
				turnRight();	
				break;		
		}
	}
	
	//=========================================ADDED=========================================

	@Override
	protected void detection () {
		freeze=false;
		// Détection des ennemis et envoi d'infos
	    for (IRadarResult o : detectRadar()) {
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            // Transmettre la position des ennemis : ENEMY dir dist type enemyX enemyY
	            double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
	            double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
	            broadcast("ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + enemyX + " " + enemyY);
	            sendLogMessage("ENEMY " + o.getObjectType() + " " + enemyX + " " + enemyY);
	        }
	    	if (o.getObjectDistance()<=100) {
	    		freeze=true;
	    	}
	    }		
	}
	
	// Interprète les messages des alliés
	private void readMessages() {
		//freeze = true;
        ArrayList<String> messages = fetchAllMessages();
        // messages affichés à l'instant T
        //if (whoAmI == NBOT) {
        //System.out.println("Lecture message " + count++);
        //System.out.println(messages);}
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
	            case "POS" :
	            	if (!turningTask) state = State.MOVING;
	            	double targetX = Double.parseDouble(parts[2]);
	                double targetY = Double.parseDouble(parts[3]);
	            	allyPos.put(parts[1], new Double[]{targetX, targetY});
	            	double distance = Math.sqrt(Math.pow(targetX - myX, 2) + Math.pow(targetY - myY, 2));
	        	    if (distance < 500 && parts[1] != "NBOT" && parts[1] != "SBOT") {
	        	        //freeze=false;
	        	        // à modifier, le bot attend que tous les shooters soient à range, cas impossible
	        	      
	        	    }
	            	break;
            }
        }
    }
	
	protected void myMove() {
		if(detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
			move(); 
            myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
            myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
    		sendMyPosition();
    		return;
		}
        state = State.TURNING_LEFT;
        turningTask = true;
        oldAngle = getHeading();
        turnLeft();
	}
	
	
}
