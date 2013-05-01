package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot;

public class RobotPosition {
	public int xPos;
	
	public int yPos;
	
	public int anglePos;
	
	/**
	 * calculate this+rhs using, returning a <b>new</b> RobotPosition
	 * @param rhs
	 * @return
	 */
	public RobotPosition Add(RobotPosition rhs) {
		RobotPosition robotPosition = new RobotPosition();
		robotPosition.xPos = this.xPos+rhs.xPos;
		robotPosition.yPos = this.yPos+rhs.yPos;
		robotPosition.anglePos = this.anglePos+rhs.anglePos;
		return robotPosition;
	}
	
	/**
	 * calculate this-rhs using, returning a <b>new</b> RobotPosition
	 * @param rhs
	 * @return
	 */
	public RobotPosition Minus(RobotPosition rhs) {
		RobotPosition robotPosition = new RobotPosition();
		robotPosition.xPos = this.xPos-rhs.xPos;
		robotPosition.yPos = this.yPos-rhs.yPos;
		robotPosition.anglePos = this.anglePos-rhs.anglePos;
		return robotPosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + anglePos;
		result = prime * result + xPos;
		result = prime * result + yPos;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RobotPosition other = (RobotPosition) obj;
		if (anglePos != other.anglePos)
			return false;
		if (xPos != other.xPos)
			return false;
		if (yPos != other.yPos)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RobotPosition [xPos=" + xPos + ", yPos=" + yPos + ", anglePos="
				+ anglePos + "]";
	}
}
