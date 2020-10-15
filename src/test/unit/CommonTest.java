package test.unit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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

	public static void main(String[] args) {
		
	}
}
