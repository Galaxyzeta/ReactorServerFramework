package test.entity;

public class CycleInjection {
	private CycleInjection2 cycleInjection;

	public void setCycleInjection(CycleInjection2 cycleInjection) {
		this.cycleInjection = cycleInjection;
	}
	public CycleInjection2 getCycleInjection() {
		return cycleInjection;
	}

	@Override
	public String toString() {
		return "cycleInjection 1";
	}
}
