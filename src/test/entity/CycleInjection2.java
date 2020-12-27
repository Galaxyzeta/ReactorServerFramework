package test.entity;

public class CycleInjection2 {
	private CycleInjection cycleInjection;

	public CycleInjection getCycleInjection() {
		return cycleInjection;
	}
	public void setCycleInjection(CycleInjection cycleInjection) {
		this.cycleInjection = cycleInjection;
	}

	@Override
	public String toString() {
		return "CycleInjection 2";
	}
}
