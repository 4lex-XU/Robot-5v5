/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: supportGUI/FileLoader.java 2014-10-28 buixuan.
 * ******************************************************/
package supportGUI;

import characteristics.IBrain;
import characteristics.Parameters;

public class FileLoader {
  //---CONSTRUCTORS---//
  public FileLoader(){}
  
  //---GET/SETTERS---//
  public IBrain getTeamAMainBotBrain() {
    return HardCodedParameters.instantiate(Parameters.teamAMainBotBrainClassName, IBrain.class);
  }
  public IBrain getTeamASecondaryBotBrain() {
    return HardCodedParameters.instantiate(Parameters.teamASecondaryBotBrainClassName, IBrain.class);
  }
  public IBrain getTeamBMainBotBrain() {
    return HardCodedParameters.instantiate(Parameters.teamBMainBotBrainClassName, IBrain.class);
  }
  public IBrain getTeamBSecondaryBotBrain() {
    return HardCodedParameters.instantiate(Parameters.teamBSecondaryBotBrainClassName, IBrain.class);
  }
  public String getTeamAMainBotAvatarFileName() {
    return Parameters.teamAMainBotAvatar;
  }
  public String getTeamASecondaryBotAvatarFileName() {
    return Parameters.teamASecondaryBotAvatar;
  }
  public String getTeamBMainBotAvatarFileName() {
    return Parameters.teamBMainBotAvatar;
  }
  public String getTeamBSecondaryBotAvatarFileName() {
    return Parameters.teamBSecondaryBotAvatar;
  }
  public String getTeamAName() {
    return Parameters.teamAName;
  }
  public String getTeamBName() {
    return Parameters.teamBName;
  }
  public double getTeamAMainBotSpeed() {
    return Parameters.teamAMainBotSpeed;
  }
  public double getTeamASecondaryBotSpeed() {
    return Parameters.teamASecondaryBotSpeed;
  }
  public double getTeamBMainBotSpeed() {
    return Parameters.teamBMainBotSpeed;
  }
  public double getTeamBSecondaryBotSpeed() {
    return Parameters.teamBSecondaryBotSpeed;
  }
}
