import com.example.ppark.ParkState;
import java.lang.Math;

class Park {
    static ParkState stage = ParkState.OFF;
    static double spaceSize = 0.0;
    static double arcDist = 0.0;
    static double offsetDist = 0.0;

    /* defaults are scion FR-S */
    private double carLength = 4.27, 
	wheelBase= 2.58, 
	turningCircle = 5.4,
	frontOH = 0.7,
	carWidth = 1.78,
	alignDist = 0.7,
	minSpace = 5.94,
	turnCircum = 5.5*Math.PI/4;
    
    public Park (double carLength, 
		 double wheelBase, 
		 double turningCircle, 
		 double frontOH,
		 double carWidth,
		 double alignDist) {

	this.carLength = carLength;
	this.wheelBase = wheelBase;
	this.turningCircle = turningCircle;
	this.frontOH = frontOH;
	this.carWidth = carWidth;
	this.alignDist = alignDist;
	this.turnCircum = turningCircle*Math.PI/4;
	this.minSpace = 5.94;
	
	/* carLength - wheelBase - frontOH + sqrt((pow(turningCircle,2) - pow(wheelBase,2) + pow((wheelBase+FrontOH),2) - pow(
	   (sqrt(pow(turningCircle,2) - 
	   pow(wheelBase,2))-carWidth),2)))
	   
	 */


	Park.spaceSize = 0.0;
	Park.stage = ParkState.MEASURE;
    }

    public Park () {
	Park.spaceSize = 0.0;
	Park.stage = ParkState.MEASURE;
    }

    public int monitor (int steeringWheelAngle, 
			int velocity, 
			int acceleration,
			int gear,
			int deltaT) {
	switch(stage) {
	case MEASURE: return measure(velocity, acceleration, gear, deltaT);

	case ALIGN: return align(velocity, acceleration, gear, deltaT);

        case RIGHTLOCK: return rightLock(steeringWheelAngle, velocity, acceleration, gear, deltaT);

	case LEFTLOCK: return leftLock(steeringWheelAngle, velocity, acceleration, gear, deltaT);

	default: Park.stage = ParkState.OFF;
	    return 7;

	}
    }

    public void setStage(ParkState state) throws ParkingException {
	if (stage == ParkState.MEASURE) {
	    if (Park.spaceSize < minSpace) {
		Park.spaceSize = 0;
		Park.stage = ParkState.OFF;
		throw(new ParkingException("Space too small"));
	    }
	}
	Park.stage = state;
    }
    
    private int measure(int velocity, int acceleration, int gear,int deltaT ){
	Park.spaceSize += (12 == gear)? -(velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001) : velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001; 
	return 1;

    }

    private int align (int velocity, int acceleration, int gear, int deltaT){
	Park.offsetDist += (12 == gear)? -(velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001) : velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001;
	if (alignDist < Park.offsetDist) return 1;
	
	if (alignDist > Park.offsetDist){
	    Park.offsetDist = 0.0;
	    Park.stage = ParkState.RIGHTLOCK;
	    return 4;
	}


    }


    private int rightLock(int steeringAngle, int velocity, int acceleration, int gear, int deltaT){
	Park.arcDist += (12 == gear)? -(velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001) : velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001;
	if (turnCircum < Park.arcDist) return 4;
	
	    Park.stage = ParkState.LEFTLOCK;
	    return 6;
    }
    
    private int leftLock(int steeringAngle, int velocity, int acceleration, int gear, int deltaT){
    Park.arcDist += (12 == gear)? (velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001) : -velocity*deltaT*0.001+ 0.5*acceleration*deltaT*deltaT*0.0000001;
	if (Park.arcDist > 0 ) return 6;
	
	    Park.stage = ParkState.OFF;
	    return 8;

    }
}
    

