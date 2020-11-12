package test.pojo;

public class MyHttpResult {
	public Integer code;
	public String status;
	public Object data;

	public MyHttpResult(Integer code, String status, Object data) {
		this.code = code;
		this.status = status;
		this.data = data;
	}

	public Integer getCode() {
		return code;
	}
	public String getstatus() {
		return status;
	}
	public Object getData() {
		return data;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	public void setstatus(String status) {
		this.status = status;
	}
	public void setData(Object data) {
		this.data = data;
	}
}
