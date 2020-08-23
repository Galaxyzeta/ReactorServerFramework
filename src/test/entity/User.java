package test.entity;

/**
 * 测试类 - Ioc
 * 此类将被作为普通 entity
 */
public class User {

	private Address addr;
	private String username;
	private String password;

	public User() {}

	// getter
	public Address getAddress() {
		return addr;
	}
	public String getPassword() {
		return password;
	}
	public String getUsername() {
		return username;
	}

	// setter
	public void setAddress(Address addr) {
		this.addr = addr;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	// Ioc init method
	public void init() {
		System.out.println("User init.");
	}
}