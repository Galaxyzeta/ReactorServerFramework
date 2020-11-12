package test.pojo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryList {
	
	public List<String> list;
	public Set<String> set;
	public Map<String, Double> map;
	public Integer[] arr;
	public int[] primitiveArr;

	// === Getter ===
	public Integer[] getArr() {
		return arr;
	}
	public List<String> getList() {
		return list;
	}
	public Map<String, Double> getMap() {
		return map;
	}
	public Set<String> getSet() {
		return set;
	}
	public int[] getPrimitiveArr() {
		return primitiveArr;
	}

	// === Setter ===
	public void setList(List<String> list) {
		this.list = list;
	}
	public void setArr(Integer[] arr) {
		this.arr = arr;
	}
	public void setMap(Map<String, Double> map) {
		this.map = map;
	}
	public void setSet(Set<String> set) {
		this.set = set;
	}
	public void setPrimitiveArr(int[] primitiveArr) {
		this.primitiveArr = primitiveArr;
	}
}
