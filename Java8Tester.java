import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Java8Tester {

	public static void main(String[] args) {
		Java8Tester tester = new Java8Tester();
		tester.sort();
		tester.mathAndGreet();
		
		StringBuffer sb = new StringBuffer();
		sb.append("hello" );
		System.out.println(sb.toString());
	}

	private void sort() {
		List<String> names1 = new ArrayList<String>();
		names1.add("Mahesh ");
		names1.add("Suresh ");
		names1.add("Ramesh ");
		names1.add("Naresh ");
		names1.add("Kalpesh ");

		List<String> names2 = new ArrayList<String>();
		names2.add("Mahesh ");
		names2.add("Suresh ");
		names2.add("Ramesh ");
		names2.add("Naresh ");
		names2.add("Kalpesh ");
		
		System.out.println("Java 7 syntax : ");
		sortUsingJava7(names1);
		System.out.println(names1);

		System.out.println("Java 8 syntax : ");
		sortUsingJava8(names2);
		System.out.println(names2);
	}
	
	private void sortUsingJava7(List<String> names) {
		Collections.sort(names, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}
		});
	}
	
	private void sortUsingJava8(List<String> names) {
		Collections.sort(names, (s1, s2) -> s1.compareTo(s2));
	}
	
	private void mathAndGreet() {
		MathOperation addition = (a, b) -> a+b;
		MathOperation substract = (int a, int b) -> a-b;
		MathOperation multiple = (a,b) -> {return a*b;};
		MathOperation division = (a,b) -> a/b;
		MathOperation power = (a,b) -> {int x=a*a+b*b; return x;};
		
		System.out.println("10 + 5 = "+operate(10, 5, addition));
		System.out.println("10 - 5 = "+operate(10, 5, substract));
		System.out.println("10 x 5 = "+operate(10, 5, multiple));
		System.out.println("10 / 5 = "+operate(10, 5, division));
		System.out.println("10*10 + 5*5 = "+operate(10, 5, power));
		
		String greet = "Hello";
		GreetingService greetService1 = a -> System.out.println(greet+" " +a);
		GreetingService greetService2 = a -> System.out.println(greet+"2 " +a);
		
		greetService1.sayMessage("john");
		greetService2.sayMessage("grace");
	}
	
	interface MathOperation {
		int operation(int a, int b);
	}
	
	interface GreetingService {
		void sayMessage(String message);
	}
	
	private int operate(int a, int b, MathOperation mathOperation) {
		return mathOperation.operation(a, b);
	}
}
