package algorithms;

import java.util.HashSet;
import java.util.Set;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

public class SecondaryMacDuo extends Brain{

	private static final double ANGLEPRECISION = 0.1;
    private static final String NBOT = "NBOT";
    private static final String SBOT = "SBOT";	
    private static final int TOTAL_SHOOTERS = 3;

    private static final int PHASE1_RDV_POINT = 1;
    private static final int PHASE2_WAITING = 21;
    private static final int PHASE2_MOVING = 22;
    
    // points de rdv
    private static final double TARGET_X1 = 600; // TargetX2 - 300
    private static final double TARGET_Y1 = 840; // TargetY2 - 660
    
    private static final double TARGET_X2 = 900; 
    private static final double TARGET_Y2 = 1500;
    
    //---VARIABLES---//
    private int state;
	private String whoAmI;
	private double myX,myY;
	private boolean isMoving;
	
	// Stocker les tireurs qui ont envoyé "READY"
	private Set<String> readyShooters = new HashSet<>();
	
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
	    state = PHASE1_RDV_POINT;
	}

	@Override
	public void step() {
		detectAndBroadCast();
		
		if(state == PHASE1_RDV_POINT) {
			moveToTarget();
			return;
		}

		/*
	    int shooterReadyCount = readyShooters.size();

	    // Détection des obstacles devant
		if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
	        isMoving = false;
	        return; 
	    }

	    // Déplacement vers la droite
		if (isMoving) {
	        move();
	        myX += Parameters.teamASecondaryBotSpeed;
	        //sendLogMessage("Tous les tireurs sont prêts, j'avance !");
	    } else {
	        //sendLogMessage("En attente des tireurs...");
	    }*/
	}
	
	private void phase1() {
		
	}
	
	private void moveToTarget() {
	    double tX = (whoAmI == SBOT) ? TARGET_X2 : TARGET_X1;
	    double tY = (whoAmI == SBOT) ? TARGET_Y2 : TARGET_Y1;

	    double dx = tX - myX;
	    double dy = tY - myY;
	    double angleToTarget = Math.atan2(dy, dx);

	    if (!isSameDirection(getHeading(), angleToTarget)) {
	        //  Étape 1 : Tourner progressivement vers la cible
	        turnTo(angleToTarget);
	        sendLogMessage("Rotation vers la cible...");
	    } else {
	        //  Étape 2 : Avancer quand l'angle est correct
	        sendLogMessage("Angle correct, déplacement...");
	        move();
	        
	        // Mise à jour des coordonnées
	        myX += Math.cos(getHeading()) * Parameters.teamASecondaryBotSpeed;
	        myY += Math.sin(getHeading()) * Parameters.teamASecondaryBotSpeed;
	    }

	    if (Math.abs(myX - tX) < 5 && Math.abs(myY - tY) < 5) {
	        sendLogMessage("Position cible atteinte !");
	        state = PHASE2_WAITING;
	    }
	}
	
	private void checkReadyShooters() {
		for (String msg : fetchAllMessages()) {
	        if (msg.startsWith("READY")) { 
	            String[] parts = msg.split(" ");
	            if (parts.length > 1) {
	                String shooterID = parts[1];  // L'identifiant du tireur

	                if (!readyShooters.contains(shooterID)) {
	                    readyShooters.add(shooterID);  // Ajouter une seule fois
	                }
	            }
	        }
	    }
	}
	
	private void detectAndBroadCast() {
		// Détection des ennemis et envoi d'infos
	    for (IRadarResult o : detectRadar()) {
	    	if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || 
	            o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
	            
	            // Transmettre la position des ennemis : ENEMY dir dist type myX myY
	            String message = "ENEMY " + o.getObjectDirection() + " " + o.getObjectDistance() + " " + o.getObjectType() + " " + myX + " " + myY;
	            broadcast(message);
	            sendLogMessage(message);
	        }
	    }
	}
	
	private boolean isSameDirection(double dir1, double dir2) {
	    double diff = normalize(dir1 - dir2);

	    // Ajuster pour prendre l'angle le plus court
	    if (diff > Math.PI) diff -= 2 * Math.PI;
	    if (diff < -Math.PI) diff += 2 * Math.PI;

	    return Math.abs(diff) < ANGLEPRECISION;
	}
	
	private double normalize(double dir){
	    double res=dir;
	    while (res<0) res+=2*Math.PI;
	    while (res>=2*Math.PI) res-=2*Math.PI;
	    return res;
	}
	
	private boolean isHeading(double dir){
		return Math.abs(Math.sin(getHeading()-dir))<ANGLEPRECISION;
	}
	
	protected void turnTo(double targetAngle) {
	    double currentAngle = getHeading();
	    double diff = normalize(targetAngle - currentAngle);

	    if (diff > Math.PI) {
	        diff -= 2 * Math.PI;  // Tourner dans l’autre sens
	    } else if (diff < -Math.PI) {
	        diff += 2 * Math.PI;  // Tourner dans l’autre sens
	    }

	    // 🏁 Tourner dans la bonne direction
	    if (diff > ANGLEPRECISION) {
	        stepTurn(Parameters.Direction.RIGHT);
	    } else if (diff < -ANGLEPRECISION) {
	        stepTurn(Parameters.Direction.LEFT);
	    }
	}

}
