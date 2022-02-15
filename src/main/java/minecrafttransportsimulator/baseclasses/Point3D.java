package minecrafttransportsimulator.baseclasses;

/**Basic 3D point class.  Allows for saving of positions in a less recourse-heavy
 * format than Minecraft's vectors.  This class is mutable to allow
 * the point to change, cause we don't need to create a new point every time we
 * move a thing.  As this point can be used for vectors, methods exist for vector
 * operations such as dot product, cross product, and rotation.
 * Note that all methods return this object for nested operations, unless otherwise
 * specified.
 *
 * @author don_bruce
 */
public class Point3D{
	
	public double x;
	public double y;
	public double z;
	
	public Point3D(){
		this(0, 0, 0);
	}
	
	public Point3D(double x, double y, double z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public boolean equals(Object object){
		//TODO see if this is needed anymore, or if FPEs aren't a thing.
		if(object instanceof Point3D){
			Point3D otherPoint = (Point3D) object;
			return (float)x == (float)otherPoint.x && (float)y == (float)otherPoint.y && (float)z == (float)otherPoint.z;
		}else{
			return false;
		}
	}
	
	@Override
	public String toString(){
		return "[X:" + x + ", Y:" + y + ", Z:" + z + "]";
	}
	
	/**
	 * Sets the point to the passed-in values.
	 */
	public Point3D set(double x, double y, double z){
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	/**
	 * Sets the point to the passed-in point.
	 */
	public Point3D set(Point3D point){
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		return this;
	}
	
	/**
	 * Adds the passed-in values to the point.
	 */
	@SuppressWarnings("hiding")
	public Point3D add(double x, double y, double z){
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	/**
	 * Adds the passed-in point's values to this point.
	 */
	public Point3D add(Point3D point){
		this.x += point.x;
		this.y += point.y;
		this.z += point.z;
		return this;
	}
	
	/**
	 * Adds the scaled value of the scale multiplied by the 
	 * passed-in vector to this point.  This is useful
	 * if you don't want to modify the vector, but want 
	 * to translate along it's path.
	 */
	public Point3D addScaled(Point3D point, double scale){
		this.x += point.x*scale;
		this.y += point.y*scale;
		this.z += point.z*scale;
		return this;
	}
	
	/**
	 * Subtracts the passed-in point's values from this point.
	 */
	public Point3D subtract(Point3D point){
		this.x -= point.x;
		this.y -= point.y;
		this.z -= point.z;
		return this;
	}
	
	/**
	 * Scales all values of this point by the passed-in factor.
	 */
	public Point3D scale(double scale){
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	/**
	 * Multiplies all values of this point by the values of the passed-in point.
	 */
	public Point3D multiply(Point3D point){
		this.x *= point.x;
		this.y *= point.y;
		this.z *= point.z;
		return this;
	}
	
	/**
	 * Sets the point to the interpolation between itself, and the passed-in point,
	 * with the distance passed-in.
	 */
	public Point3D interpolate(Point3D point, double distance){
		this.x += (point.x - x)*distance;
		this.y += (point.y - y)*distance;
		this.z += (point.z - z)*distance;
		return this;
	}
	
	/**
	 * Inverts the sign on this point.  This should be used instead of multiplying
	 * by -1 as it's quicker and more accurate.
	 */
	public Point3D invert(){
		this.x = -x;
		this.y = -y;
		this.z = -z;
		return this;
	}
	
	/**
	 * Returns the distance between this point and the passed-in point.
	 */
	public double distanceTo(Point3D point){
		double deltaX = point.x - this.x;
		double deltaY = point.y - this.y;
		double deltaZ = point.z - this.z;
		return Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
	}
	
	/**
	 * Returns true if the distance between this point and the passed-in point
	 * is less than the distance specified.  This is an optimized method of
	 * {@link #distanceTo(Point3D)} as it doesn't do square root calls.
	 */
	public boolean isDistanceToCloserThan(Point3D point, double distance){
		double deltaX = point.x - this.x;
		double deltaY = point.y - this.y;
		double deltaZ = point.z - this.z;
		return deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ < distance*distance;
	}
	
	/**
	 * Returns the dot product between this point and the passed-in point.
	 */
	public double dotProduct(Point3D point){
		return this.x*point.x + this.y*point.y + this.z*point.z;
	}
	
	/**
	 * Returns the cross product between this point and the passed-in point.
	 * Return value is a new point that is the cross product of the object
	 * this was invoked on, and the passed-in object.  Neither object is
	 * modified by this operation.
	 */
	public Point3D crossProduct(Point3D point){
		return new Point3D(this.y*point.z - this.z*point.y, this.z*point.x - this.x*point.z, this.x*point.y - this.y*point.x);
	}
	
	/**
	 * Returns the length of this point as if it was a vector.
	 */
	public double length(){
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	/**
	 * Normalizes this point to be a unit vector.
	 */
	public Point3D normalize(){
		double length = length();
		if(length > 1.0E-8D){
			x /= length;
			y /= length;
			z /= length;
		}
		return this;
	}
	
	/**
     * Returns the difference between the passed-in value and this point's Y value, between
     * the range of -180 to 180.  Placed here as Y is frequently used in yaw angle for in-game
     * entities and needs to be clamped to this domain for calculations.
     */
    public double getClampedYDelta(double otherY){
    	double deltaYaw = this.y - otherY;
		while(deltaYaw > 180){
			deltaYaw -= 360;
		}
		while(deltaYaw < -180){
			deltaYaw += 360;
		}
		return deltaYaw;
    }
    
    /**
	 * Sets this point to the angle values defined by it.  If the point is not normalized,
	 * pass in true to the boolean to perform this operation.
	 * Note that since there is no "roll" for vectors, the z-value will always be 0.
	 * Returns the called object for nested operations.
	 */
	public Point3D getAngles(boolean normalize){
		if(normalize){
			normalize();
		}
		double theta = Math.asin(y);
		double phi = Math.atan2(x, z);
		//TODO Positive acos for theta maybe?  Other code shows this.
		//Or maybe this?
		//-Math.toDegrees(Math.atan2(motion.y, Math.hypot(motion.x, motion.z)));
		set(-Math.toDegrees(theta), Math.toDegrees(phi), 0);
		return this;
	}
	
	/**
	 * Returns a copy of this point as a new object.
	 */
	public Point3D copy(){
		return new Point3D(this.x, this.y, this.z);
	}
	
	/**
	 * Returns true if this point is 0,0,0.
	 */
	public boolean isZero(){
		return x == 0 && y == 0 && z == 0;
	}
	
	/**
     * Returns a new point with the x value equal to the second parameter, provided the X value
     * is between this point and the passed-in point, and the passed-in point's x-value is not
     * equal to this point's x-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3D getIntermediateWithXValue(Point3D endPoint, double targetX){
    	Point3D delta = endPoint.copy().subtract(this);
        if(delta.x*delta.x < 1.0E-7D){
        	//Point delta is 0, so there's no difference here.
            return null;
        }else{
        	//Return point as a factored-percentage of total length.
        	double factor = (targetX - this.x)/delta.x;
            return factor >= 0.0D && factor <= 1.0D ? delta.scale(factor).add(this) : null;
        }
    }

    /**
     * Returns a new point with the y value equal to the second parameter, provided the Y value
     * is between this point and the passed-in point, and the passed-in point's y-value is not
     * equal to this point's y-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3D getIntermediateWithYValue(Point3D endPoint, double targetY){
    	Point3D delta = endPoint.copy().subtract(this);
        if(delta.y*delta.y < 1.0E-7D){
        	//Point delta is 0, so there's no difference here.
            return null;
        }else{
        	//Return point as a factored-percentage of total length.
        	double factor = (targetY - this.y)/delta.y;
            return factor >= 0.0D && factor <= 1.0D ? delta.scale(factor).add(this) : null;
        }
    }
    
    /**
     * Returns a new point with the z value equal to the second parameter, provided the Z value
     * is between this point and the passed-in point, and the passed-in point's z-value is not
     * equal to this point's z-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3D getIntermediateWithZValue(Point3D endPoint, double targetZ){
    	Point3D delta = endPoint.copy().subtract(this);
        if(delta.z*delta.z < 1.0E-7D){
        	//Point delta is 0, so there's no difference here.
            return null;
        }else{
        	//Return point as a factored-percentage of total length.
        	double factor = (targetZ - this.z)/delta.z;
            return factor >= 0.0D && factor <= 1.0D ? delta.scale(factor).add(this) : null;
        }
    }
	
	private double lastCalcX;
	private double lastCalcY;
	private double lastCalcZ;
	private boolean calcedOnce;
	private final double[][] rotationMatrixFine = new double[3][3];
	/**
     * Rotates this point about the passed-in angles.  Rotation is done using actual sin
     * and cos calls via a rotation matrix.  This matrix is cached in this point until the point
     * is changed, so repeated uses will be faster if you don't create new "angle" objects.
     */
	public Point3D rotateFine(Point3D angles){
		if(!angles.isZero()){
			//Check if we need to create the matrix for the angles.
			if(angles.lastCalcX != angles.x || angles.lastCalcY != angles.y || angles.lastCalcZ != angles.z || !angles.calcedOnce){
				double cosX = Math.cos(Math.toRadians(angles.x));//A
				double sinX = Math.sin(Math.toRadians(angles.x));//B
				double cosY = Math.cos(Math.toRadians(angles.y));//C
				double sinY = Math.sin(Math.toRadians(angles.y));//D
				double cosZ = Math.cos(Math.toRadians(angles.z));//E
				double sinZ = Math.sin(Math.toRadians(angles.z));//F
				angles.rotationMatrixFine[0][0] = cosY*cosZ-sinX*-sinY*sinZ;
				angles.rotationMatrixFine[0][1] = -sinX*-sinY*cosZ-cosY*sinZ;
				angles.rotationMatrixFine[0][2] = -cosX*-sinY;
				angles.rotationMatrixFine[1][0] = cosX*sinZ;
				angles.rotationMatrixFine[1][1] = cosX*cosZ;
				angles.rotationMatrixFine[1][2] = -sinX;
				angles.rotationMatrixFine[2][0] = -sinY*cosZ+sinX*cosY*sinZ;
				angles.rotationMatrixFine[2][1] = sinX*cosY*cosZ+sinY*sinZ;
				angles.rotationMatrixFine[2][2] = cosX*cosY;
				
				angles.lastCalcX = angles.x;
				angles.lastCalcY = angles.y;
				angles.lastCalcZ = angles.z;
				angles.calcedOnce = true;
			}
			set(	x*angles.rotationMatrixFine[0][0] + y*angles.rotationMatrixFine[0][1] + z*angles.rotationMatrixFine[0][2],
					x*angles.rotationMatrixFine[1][0] + y*angles.rotationMatrixFine[1][1]	+ z*angles.rotationMatrixFine[1][2],
					x*angles.rotationMatrixFine[2][0] + y*angles.rotationMatrixFine[2][1] + z*angles.rotationMatrixFine[2][2]
			);
		}
		return this;
	}
	
	/**
     * Rotates this point about the passed-in angle on the Y-axis.  Useful for single-plane rotations,
     * as the Y=axis is also the first rotation to be performed on a point in all systems.
     * Uses "fine" rotation calculations.
     */
	public Point3D rotateY(double angle){
		if(angle != 0){
			double cosY = Math.cos(Math.toRadians(angle));//C
			double sinY = Math.sin(Math.toRadians(angle));//D
			set(x*cosY + z*sinY,
				y,
				x*-sinY	+ z*cosY
			);
		}
		return this;
	}
	
	/*For reference, here are the rotation matrixes.
	 * Note that the resultant rotation matrix follows the Yaw*Pitch*Roll format.
	 * Rx=[[1,0,0],[0,cos(P),-sin(P)],[0,sin(P),cos(P)]]
	 * Ry=[[cos(Y),0,sin(Y)],[0,1,0],[-sin(Y),0,cos(Y)]]
	 * Rz=[[cos(R),-sin(R),0],[sin(R),cos(R),0],[0,0,1]]
	 * {[C,0,-D],[0,1,0],[D,0,C]}*{[1,0,0],[0,A,-B],[0,B,A]}*{[E,-F,0],[F,E,0],[0,0,1]}
	 */

	
	/**
	 * Rotates the point per the passed-in matrix.
	 */
	public Point3D rotate(RotationMatrix matrix){
		double tx = matrix.m00* x + matrix.m01*y + matrix.m02*z;
		double ty = matrix.m10* x + matrix.m11*y + matrix.m12*z;
		z = matrix.m20* x + matrix.m21*y + matrix.m22*z;
		x = tx;
		y = ty;
		return this;
	}
	
	/**
	 * Aligns this point to the passed-in matrix origin.  Essentially, this leaves
	 * the point in its current position, but changes the coordinate system
	 * to be aligned to the coordinate system of this matrix.
	 * More specifically, this is an inverted rotation by the transpose of the matrix.
	 */
	public Point3D reOrigin(RotationMatrix matrix){
		double tx = matrix.m00* x + matrix.m10*y + matrix.m20*z;
		double ty = matrix.m01* x + matrix.m11*y + matrix.m21*z;
		z = matrix.m02* x + matrix.m12*y + matrix.m22*z;
		x = tx;
		y = ty;
		return this;
	}

	/**
	 * Transforms this point to align with the passed-in transformation matrix.
	 */
	public Point3D transform(TransformationMatrix matrix){
        double tx = matrix.m00*x + matrix.m01*y + matrix.m02*z + matrix.m03;
        double ty = matrix.m10*x + matrix.m11*y + matrix.m12*z + matrix.m13;
        z =  matrix.m20*x + matrix.m21*y + matrix.m22*z + matrix.m23;
        x = tx;
        y = ty;
		return this;
	}
}
