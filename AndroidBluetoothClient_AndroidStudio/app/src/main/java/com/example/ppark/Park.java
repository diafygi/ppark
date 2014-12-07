import ParkState;

class Park {
    static enum ParkState stage = ParkState.OFF
    static double parkSpaceSize = 0.0;
    static double travel distance = 0.0;
    static double minParkSpace = 10.0

    /* defaults are scion FR-S */
    private double carLength = 4250.0, 
	wheelBase=2560.0, 
	turningCircle = 5.5, 
	frontOH = 0.7,
	carWidth = 2.0;
    
    public Park (double carLength, 
		 double wheelBase, 
		 double turningCircle, 
		 double frontOH,
		 double carWidth) {

	this.carLength = carLength;
	this.wheelBase = wheelBasel
	this.turningCirlce = turningCirlce;
	this.frontOH = frontOH;
	this.carWidth = carWidth;
	Park.minParkSpace  = Park.computeMinSpace();

	Park.parkSpace = 0.0;
	Park.stage = ParkState.MEASURE;
    }

    public Park () {
	Park.parkSpace = 0.0;
	Park.stage = ParkState.MEASURE;
    }

    public int monitor (int steeringWheelAngle, 
			int velocity, 
			int acceleration) {
	switch(state) {
	MEASURE: return measure(velocity, acceleration);
	ALIGN: return align(velocity, acceleration);
        RIGHTLOCK: return rightLock(steeringWheel, velocity, acceleration);
	LEFTLOCK: return leftLock(steeringWheel, velocity, acceleration);
	defualt: return off();

	}
    }

    public static void setStage(ParkState state) throws ParkingException {
	if (state = ParkState.ALIGN) {
	     
	}
	PPark.parkStage = state;
    }
    
    private int measure(velocity, acceleration){
    

    }

    private int align (velocity, acceleration){
    

    }

    private int rightLock(velocity, acceleration){
    

    }
    
    private int leftLock(velocity, acceleration){
    

    }

    static double computeMinSpace () {
	
    }
}
    

