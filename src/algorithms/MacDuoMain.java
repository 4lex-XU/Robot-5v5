package algorithms;

import java.util.ArrayList;


import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;


//============================================================================
//====================================MAIN====================================
//============================================================================

public class MacDuoMain extends MacDuoBaseBot {
	
	private static final double FIREANGLEPRECISION = Math.PI/(double)6;

    
    // les ids des shooters
    private static final String MAIN1 = "1";
    private static final String MAIN2 = "2";
    private static final String MAIN3 = "3";
    
    
    //---VARIABLES---//
    private double rdvX, rdvY;  
    private double targetX, targetY;  
    private boolean friendlyFire;
    private boolean fireOrder;
    private int fireRythm,rythm,counter;
    private int countDown;


 	
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
	    isMoving=true;
	    state = State.MOVING;
	    rdv_point = true;
    }
    
    @Override
    public void step() {
    	
    	//DEBUG MESSAGE
        boolean debug=true;
        if (debug && whoAmI == MAIN1) {
        	sendLogMessage("#MAIN1 *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
        }
        if (debug && whoAmI == MAIN2) {
        	sendLogMessage("#MAIN2 *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
        }
        if (debug && whoAmI == MAIN3) {
        	sendLogMessage("#MAIN3 *thinks* (x,y)= ("+(int)myX+", "+(int)myY+") theta= "+(int)(myGetHeading()*180/(double)Math.PI)+"Â°. #State= "+state);
        }
        //if (debug && fireOrder) sendLogMessage("Firing enemy!!");
        
    	detection();
		readMessages();
		reach_rdv_point(rdvX, rdvY);
		
		if (freeze) return;
		
		switch (state) {
			case FIRE :
				handleFire();
				break;
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
	    	if (o.getObjectDistance()<=100 && !isRoughlySameDirection(o.getObjectDirection(),getHeading()) && o.getObjectType()!=IRadarResult.Types.BULLET) {
	    		freeze=true;
	        }
	        if (o.getObjectType()==IRadarResult.Types.TeamMainBot || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot || o.getObjectType()==IRadarResult.Types.Wreck) {
	          if (fireOrder && onTheWay(o.getObjectDirection())) {
	            friendlyFire=false;
	          }
	        }
	    }		
	}
    
    private void readMessages() {
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            String[] parts = msg.split(" ");
            switch (parts[0]) {
	            case "POS" :
	            	double targetX = Double.parseDouble(parts[2]);
	                double targetY = Double.parseDouble(parts[3]);
	            	allyPos.put(parts[1], new Double[]{targetX, targetY});
	            	break;
                case "ENEMY":
                	//sendLogMessage("enemy message received");
                    handleEnemyMessage(parts);
                    break;
             
                case "SCOUT_DOWN_A":
                case "SCOUT_DOWN_B":
                    break;
            }
        }
    }

    private void handleEnemyMessage(String[] parts) {
    	fireOrder=true;
        double enemyX = Double.parseDouble(parts[4]);
        double enemyY = Double.parseDouble(parts[5]);

        double dx = enemyX - myX;
        double dy = enemyY - myY;

        double distanceEnemyMe = Math.sqrt(dx * dx + dy * dy);
        //sendLogMessage("distanceEnemyMe " + distanceEnemyMe);
        if (distanceEnemyMe <= 900) {
            state = State.FIRE;
        } else {
        	if (!turningTask) state = State.MOVING;
            targetX = enemyX;
            targetY = enemyY;
        }
    }
    
    private void handleFire () {
    	//sendLogMessage("Firing at target: " + targetX + ", " + targetY);
        
        // Vérifier si on a bien une cible
        if (targetX != 0 && targetY != 0) {
            double dx = targetX - myX;
            double dy = targetY - myY;

            // Vérifier qu'on ne tire pas sur un allié
            if (friendlyFire) {
                //sendLogMessage("Friendly fire detected! Holding fire.");
                state = State.MOVING;
                return;
            }

            // Tirer sur l'ennemi
            firePosition(targetX, targetY);

            // Vérifier la distance après tir
            double distanceEnemyMe = Math.sqrt(dx * dx + dy * dy);
            if (distanceEnemyMe > 900) {
                state = State.MOVING;
                //sendLogMessage("Enemy out of range, switching to MOVING.");
            }
        } else {
            //sendLogMessage("No target available, switching to MOVING.");
            state = State.MOVING;
        }
    }
    
	
	protected void myMove() {
	    if (!rdv_point && !(detectFront().getObjectType()==IFrontSensorResult.Types.NOTHING) ) {
	    	state = State.TURNING_LEFT;
	    	oldAngle = myGetHeading();
	    	turningTask = true;
	    	stepTurn(Parameters.Direction.LEFT);
	    	return;
	    }
		move();
        myX += Math.cos(getHeading()) * Parameters.teamAMainBotSpeed;
        myY += Math.sin(getHeading()) * Parameters.teamAMainBotSpeed;
        sendMyPosition();
	}
	
	private void firePosition(double x, double y){
	    if (myX<=x) fire(Math.atan((y-myY)/(double)(x-myX)));
	    else 		fire(Math.PI+Math.atan((y-myY)/(double)(x-myX)));
	    return;
	 }
	
	private boolean isRoughlySameDirection(double dir1, double dir2){
	    return Math.abs(normalize(dir1)-normalize(dir2))<FIREANGLEPRECISION;
	}
	
	private boolean onTheWay(double angle){
	    if (myX<=targetX) return isRoughlySameDirection(angle,Math.atan((targetY-myY)/(double)(targetX-myX)));
	    else return isRoughlySameDirection(angle,Math.PI+Math.atan((targetY-myY)/(double)(targetX-myX)));
	  }

}
