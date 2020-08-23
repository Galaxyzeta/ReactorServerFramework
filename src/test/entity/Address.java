package test.entity;

/**
 * 测试类 - Ioc 容器
 * 此类将被 User 类依赖
 */
public class Address {
	private String street;
	private String city;

	public Address() {}

	public Address(String street, String city) {
		this.street = street;
		this.city = city;
	}

	public void setCity(String city) {
		this.city = city;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getCity() {
		return city;
	}
	public String getStreet() {
		return street;
	}
}