package snappy.pack;

public class SnappyCanvasStats implements ICanvasStats {

    /// <summary>
    /// Number of times an attempt was made to add an image to the canvas used by the mapper.
    /// </summary>
    public int rectangleAddAttempts;

    /// <summary>
    /// Number of cells generated by the canvas.
    /// </summary>
    public int nbrCellsGenerated;

    /// <summary>
    /// See ICanvasStats
    /// </summary>
    public int lowestFreeHeightDeficit;
    
	@Override
	public int getLowestFreeHeightDeficit() {

		return lowestFreeHeightDeficit;
	}

	@Override
	public int getNbrCellsGenerated() {

		return nbrCellsGenerated;
	}

	@Override
	public int getRectangleAddAttempts() {

		return rectangleAddAttempts;
	}

	@Override
	public void setLowestFreeHeightDeficit(int lowestFreeHeightDeficit) {

		this.lowestFreeHeightDeficit = lowestFreeHeightDeficit; 
	}

	@Override
	public void setNbrCellsGenerated(int nbrCellsGenerated) {

		this.nbrCellsGenerated = nbrCellsGenerated;
	}

	@Override
	public void setRectangleAddAttempts(int rectangleAddAttempts) {

		this.rectangleAddAttempts = rectangleAddAttempts;
	}

}