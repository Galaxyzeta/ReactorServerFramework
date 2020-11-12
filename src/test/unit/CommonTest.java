package test.unit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.parser.ViewResolver;
import com.galaxyzeta.util.ResponseFactory;

import test.pojo.MyHttpResult;
import test.pojo.QueryList;

public class CommonTest {
	
	// 测试文件IO路径
	private void fileIODestinationTest() {
		try {
			File fp = new File("src/test/unit/testFile.txt");
			FileReader fr = new FileReader(fp);
			System.out.println("READ OK");
		} catch(IOException ioe) {
			System.out.println(ioe);
		}
	}

	// 测试递归包扫描
	private void recursivePacakgeScan() {
		
	}

	/** 测试 Jason 解析 */
	private static void viewResolveJson() {
		ViewResolver v = new ViewResolver();
		// 准备测试物体
		QueryList ql = new QueryList();
		ArrayList<String> li = new ArrayList<>();
		li.add("asd");
		li.add("qwert");
		HashMap<String, Double> map = new HashMap<>();
		map.put("IDE1", 95.0);
		map.put("IDE2", 75.6);
		ql.setList(li);
		ql.setArr(new Integer[] {1,2,3,4,5});
		ql.setMap(map);
		ql.setSet(li.stream().collect(Collectors.toSet()));
		ql.setPrimitiveArr(new int[] {1,2,3,4,5});
		MyHttpResult res = new MyHttpResult(200, "OK", ql);
		// 视图解析
		HttpResponse resp = ResponseFactory.getSuccess();
		v.resolve(res, resp);
		System.out.println(resp.getResponseBody());
	}

	public static void main(String[] args) {
		viewResolveJson();
	}
}
