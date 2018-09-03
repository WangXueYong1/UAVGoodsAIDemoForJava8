import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.hikvision.entity.Authentication;
import com.hikvision.entity.Goods;
import com.hikvision.entity.Purchase_UAV;
import com.hikvision.entity.SendObj;
import com.hikvision.entity.ShowIdentity;
import com.hikvision.entity.StepObj;
import com.hikvision.entity.UAV_enemy;
import com.hikvision.entity.UAV_info;
import com.hikvision.entity.UAV_we;
public class Main {
	static double m = Math.random();
	static ArrayList<goodpro> goodpros = new ArrayList<goodpro>();
	@SuppressWarnings("unused")
	private static ArrayList<UAV_price> UAV_priceList = null;
	static int time;
	static int h_low = 0;
	static int h_high = 0;
	static int we_value;
	static int enemy_value;
	public static final int SOCKET_HEAD_LEN = 8;
	public static Gson gson = new Gson();
	static Parking park = new Parking();
	static ArrayList<UAV_enemy_Pro> uav_enemy_proList = new ArrayList<UAV_enemy_Pro>();
	@SuppressWarnings("unused")
	private static UAV_enemy_Pro uav_enemy_Pro;
	static int[] htoc;
	static ArrayList<Point3> fogPList;
	static int fogPIndex = 0;
	static int inintUavSize = 0;

	static int[][][][][] pathA;

	public static String readFromServer(BufferedReader bufferedReader) throws IOException {
		String info = "";
		char[] buff = new char[1024];
		int length = 0;
		String length_str = null;
		bufferedReader.read(buff, 0, SOCKET_HEAD_LEN);
		length_str = new String(buff, 0, SOCKET_HEAD_LEN);
		if (length_str == null || length_str.trim().length() == 0) {
			System.out.println("读取结束，返回值为空值");
			return null;
		}
		int maxLength = Integer.valueOf(length_str);
		int currentLength = 0;
		while (currentLength < maxLength) {
			length = bufferedReader.read(buff, 0, 1024);
			info += new String(buff, 0, length);
			currentLength += length;
		}
		// System.out.println("服务器：" + length_str + info);
		return info;
	}

	public static String sendData(Object object, PrintWriter printWriter) {
		String data_str = gson.toJson(object);
		String lengthOfJSON = String.format("%08d", data_str.length());
		String sendData = lengthOfJSON + data_str;
		// System.out.println("客户端:" + sendData);
		printWriter.print(sendData);
		printWriter.flush();
		return lengthOfJSON + data_str;
	}

	// 入口函数
	public static void main(String args[]) throws Exception {
		if (args == null || args.length != 3) {
			args = new String[3];
			args[0] = "39.105.71.189";
			args[1] = "32409";
			args[2] = "4fb7570c-2896-47db-b9d8-407f0513cd78";
		}
		if (args == null || args.length != 3) {
		} else {
			String serverHost = args[0];// 主机名
			String serverPort = args[1];// 端口号
			String authToken = args[2];// 令牌
			Socket socket = new Socket(serverHost, Integer.parseInt(serverPort));
			InputStream inputStream = socket.getInputStream();// 获取一个输入流，接收服务端的信息
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);// 包装成字符流，提高效率
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);// 缓冲区

			readFromServer(bufferedReader);
			String token = authToken;
			// 根据输入输出流和服务端连接
			OutputStream outputStream = socket.getOutputStream();// 获取一个输出流，向服务端发送信息
			PrintWriter printWriter = new PrintWriter(outputStream);// 将输出流包装成打印流
			// 发送消息1 2.2 选手向裁判服务器表明身份(Player -> Judger)
			ShowIdentity showIdentity = new ShowIdentity(token, "sendtoken");
			sendData(showIdentity, printWriter);
			// 接受服务器消息2 2.3 身份验证结果(Judger -> Player)
			String temp = readFromServer(bufferedReader);
			Authentication authentication = JSONObject.parseObject(temp, Authentication.class);
			// 发送消息2 2.3.1 选手向裁判服务器表明自己已准备就绪(Player -> Judger)
			ShowIdentity showIdentity2 = new ShowIdentity(token, "ready");
			sendData(showIdentity2, printWriter);
			test(bufferedReader, printWriter);
			bufferedReader.close();
			inputStreamReader.close();
			printWriter.close();
			outputStream.close();
			socket.close();
		}
	}

	public static void test(BufferedReader bufferedReader, PrintWriter printWriter) throws Exception {
		String mapString = "";
		// 接受服务器消息3 2.4 对战开始通知(Judger -> Player) MapData
		mapString = readFromServer(bufferedReader);
		// sendData(coordinateOfUAV, printWriter); map(z x y)
		JSONObject jsonObj = JSONObject.parseObject(mapString);
		String action = "";
		String token = jsonObj.getString("token");
		JSONObject mapSize;// 地图x方向长度
		int[][] h_low_high_bildingHight = new int[2][];
		JSONObject jsonObjmap = jsonObj.getJSONObject("map");
		ArrayList<UAV_Pro> uav_ProList = new ArrayList<UAV_Pro>();
		ArrayList<UAV_price> uav_priceList = new ArrayList<UAV_price>();
		char[][][] map = initialization(jsonObjmap, park, h_low_high_bildingHight, uav_priceList, uav_ProList);
		pathA = new int[map[0].length][map[0][0].length][][][];
		int[] h_low_high = h_low_high_bildingHight[0];
		h_low = h_low_high[0];
		h_high = Math.min(h_low_high[1], map.length - 1);
		int[] bildingHight = h_low_high_bildingHight[1];
		htoc = htoc(bildingHight, h_high);
		int time = 0;
		int[] UAV_priceArr = new int[uav_priceList.size()];
		for (int i = 0; i < uav_priceList.size(); i++) {
			UAV_priceArr[i] = uav_priceList.get(i).value;
		}
		int[] uav_priceSotNum = sortNum(UAV_priceArr);
		for (int i = 0; i < uav_ProList.size(); i++) {
			uav_ProList.get(i).nextStep = new Point3(park.getX(), park.getY(), 0);
		}
		ArrayList<Goods> goods = new ArrayList<Goods>();
		sendStep(0, 0, uav_ProList, uav_priceList, uav_priceSotNum, printWriter, token);
		inintUavSize = uav_ProList.size();
		// someFogPoint(map, bildingHight);
		fogPList = new ArrayList<Point3>();
		int aaaa1 = (map[0].length - 1) / 4;
		int aaaa2 = (map[0][0].length - 1) / 4;
		fogPList.add(new Point3(aaaa1 * 2, aaaa2 * 2, h_low + 1));
		fogPList.add(new Point3(aaaa1, aaaa2, h_low + 1));
		fogPList.add(new Point3(aaaa1 * 3, aaaa2 * 3, h_low + 1));
		fogPList.add(new Point3(aaaa1, aaaa2 * 3, h_low + 1));
		fogPList.add(new Point3(aaaa1 * 3, aaaa2, h_low + 1));
		fogPList.add(new Point3(aaaa1, aaaa2 * 2, h_low + 1));
		fogPList.add(new Point3(aaaa1 * 2, aaaa2, h_low + 1));
		fogPList.add(new Point3(aaaa1 * 3, aaaa2 * 2, h_low + 1));
		fogPList.add(new Point3(aaaa1 * 2, aaaa2 * 3, h_low + 1));
		for (int i = 0; i < fogPList.size(); i++) {
			if (Math.abs(fogPList.get(i).x - park.x) < 5 && Math.abs(fogPList.get(i).y - park.y) < 5) {
				fogPList.remove(i);
			}
		}
		while (true) {
			int stat = nextTime(uav_ProList, bildingHight, htoc, map, uav_priceList, uav_priceSotNum, bufferedReader,
					printWriter);
			if (stat == 1) {
				break;
			}
		}
	}

	public static int nextTime(ArrayList<UAV_Pro> uav_ProList, int[] high, int[] htoc, char[][][] map,
			ArrayList<UAV_price> uav_priceList, int[] UAV_priceSotNum, BufferedReader bufferedReader,
			PrintWriter printWriter) throws Exception {
		System.out.println(time);
		String step = readFromServer(bufferedReader);
		StepObj jsonObj = (StepObj) JSONObject.parseObject(step, StepObj.class);
		String token = jsonObj.token;
		int match_status = (Integer) jsonObj.match_status;
		if (match_status == 1) {
			return 1;
		}
		time = (Integer) jsonObj.time;
		we_value = jsonObj.we_value;
		enemy_value = jsonObj.enemy_value;
		ArrayList<Goods> goods = (ArrayList<Goods>) jsonObj.goods;
		ArrayList<UAV_we> uav_we = (ArrayList<UAV_we>) jsonObj.UAV_we;
		ArrayList<UAV_enemy> uav_enemy = (ArrayList<UAV_enemy>) jsonObj.UAV_enemy;
		if (time == 107) {
			int aaaaa = 0;
		}
		stepInit2(map, goods, uav_priceList, uav_enemy);
		goodproInit(map, goods, high);
		stepInit(uav_ProList, uav_priceList, uav_we);
		int uavValueSum = we_value;
		for (int i = 0; i < uav_ProList.size(); i++) {
			uavValueSum += uav_ProList.get(i).value;
		}
		int uav_enemyValueSum = enemy_value;
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			uav_enemyValueSum += uav_enemy_proList.get(i).value;
		}
		distributeAttack(uav_ProList);
		// deleteSomeGoods(uav_ProList, goodpros);
		distribtEnemy(uav_ProList, high, map);
		
		distribtGoods(uav_ProList, high, map, time);
//		for (int i = 0; i < uav_ProList.size(); i++) {
//			if (i>6) {
//				uav_ProList.remove(i);i--;
//			}
//		}
		distributeNextStep(uav_ProList, htoc, map, high);
		electricityManage(uav_ProList);
		printMessage(uav_ProList);
		sendStep(uavValueSum, uav_enemyValueSum, uav_ProList, uav_priceList, UAV_priceSotNum, printWriter, token);
		enemy_getGood(high, map, time);
		getGood(uav_ProList, high, map, time);
		// foecastNNStep(map, high);
		return 0;
	}

	private static void printMessage(ArrayList<UAV_Pro> uav_ProList) {
		// for (int i = 0; i < uav_ProList.size(); i++) {
		// System.out.println(uav_ProList.get(i).uav.no + ": 电量" +
		// uav_ProList.get(i).uav.remain_electricity + " 容量"
		// + uav_ProList.get(i).capacity + " " +
		// uav_ProList.get(i).needElecticity);
		// }
		for (int i = 0; i < uav_ProList.size(); i++) {
			System.out.println(uav_ProList.get(i).uav.no + ": v:" + uav_ProList.get(i).value + " enemy:"
					+ uav_ProList.get(i).enemy + ": goodno:" + uav_ProList.get(i).uav.goods_no);
		}
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			System.out.println(uav_enemy_proList.get(i).uav.no + ": v:" + uav_enemy_proList.get(i).value);
		}
	}

	private static void electricityManage(ArrayList<UAV_Pro> uav_ProList) {
		for (int i = 0; i < uav_ProList.size(); i++) {
			if (uav_ProList.get(i).uav.remain_electricity >= uav_ProList.get(i).capacity) {
				uav_ProList.get(i).needElecticity = false;
				uav_ProList.get(i).uav.status = 0;
			}
		}
		for (int i = 0; i < uav_ProList.size(); i++) {
			if (uav_ProList.get(i).needElecticity == true && uav_ProList.get(i).uav.x == park.x
					&& uav_ProList.get(i).uav.y == park.y && uav_ProList.get(i).uav.z == 0) {
				if (uav_ProList.get(i).uav.remain_electricity < uav_ProList.get(i).capacity) {
					uav_ProList.get(i).nextStep.z = 0;
				}
			}
			if (uav_ProList.get(i).needElecticity == true && uav_ProList.get(i).nextStep.x == park.x
					&& uav_ProList.get(i).nextStep.y == park.y && uav_ProList.get(i).nextStep.z == 0) {
				uav_ProList.get(i).uav.status = 3;
				uav_ProList.get(i).uav.remain_electricity = Math.min(uav_ProList.get(i).capacity,
						uav_ProList.get(i).uav.remain_electricity + uav_ProList.get(i).charge);

			}
		}
		for (int i = 0; i < uav_ProList.size(); i++) {
			if (uav_ProList.get(i).uav.goods_no != -1) {
				uav_ProList.get(i).uav.remain_electricity -= uav_ProList.get(i).good.weight;
			}
		}
	}

	private static void distributeAttack(ArrayList<UAV_Pro> uav_ProList) {
		int goodUavNum = 0;
		for (int i = 0; i < uav_ProList.size(); i++) {
			if (uav_ProList.get(i).model == 'g') {
				goodUavNum++;
			}
		}
		if (goodUavNum > inintUavSize / 2 && uav_enemy_proList.size() > 0) {
			boolean b = true;
			if (uav_enemy_proList.size() == 1) {
				int weightenemy = uav_enemy_proList.get(0).load_weight;
				for (UAV_Pro uavpro : uav_ProList) {
					if (uavpro.load_weight >= weightenemy) {
						b = false;
					}
				}
			}
			if (b) {
				int[] weuav_VlueSortNum = uav_weValueSortNum(uav_ProList);
				int[] enemyuav_VlueSortNum = uav_enemyValueSortNum();
				for (int i = 0; i < uav_ProList.size() - 1; i++) {
					int k = uav_enemy_proList.size() - 1 - i;
					if (k < 0) {
						break;
					}
					if (uav_ProList.get(weuav_VlueSortNum[i]).value < uav_enemy_proList
							.get(enemyuav_VlueSortNum[uav_enemy_proList.size() - 1 - i]).value) {
						if (uav_ProList.get(weuav_VlueSortNum[i]).model != 'b'
								&& uav_ProList.get(weuav_VlueSortNum[i]).uav.goods_no == -1) {
							uav_ProList.get(weuav_VlueSortNum[i]).model = 'b';
						} else {
							continue;
						}
					}
					break;
				}
				// if (we_value > enemy_value) {
				// uav_ProList.get(weuav_VlueSortNum[0]).model = 'b';
				// }
			}
		}
	}

	private static void deleteSomeGoods(ArrayList<UAV_Pro> uav_ProList, ArrayList<goodpro> goodpros) {
		for (int i = 0; i < goodpros.size(); i++) {
			for (int j = 0; j < uav_enemy_proList.size(); j++) {
				Goods good = goodpros.get(i).good;
				if (good.status == 0 && uav_enemy_proList.get(j).nextStep.x == good.start_x
						&& uav_enemy_proList.get(j).nextStep.y == good.start_y
						&& uav_enemy_proList.get(j).nextStep.z < h_low) {
					good.status = 1;
					for (int e = 0; e < uav_ProList.size(); e++) {
						if (uav_ProList.get(e).uav.x == good.start_x && uav_ProList.get(e).uav.y == good.start_y
								&& uav_ProList.get(e).uav.z <= uav_enemy_proList.get(j).nextStep.z) {
							good.status = 0;
							break;
						}
					}
				}
			}
		}
	}

	private static int[] uav_enemyValueSortNum() {
		int[] value = new int[uav_enemy_proList.size()];
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			value[i] = uav_enemy_proList.get(i).value;
		}
		return sortNum(value);
	}

	private static void foecastNNStep(char[][][] map, int[] high) {
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			UAV_enemy_Pro uav_enemy_pro = uav_enemy_proList.get(i);
			if (uav_enemy_pro.nextStep == null) {
				int nx = (uav_enemy_pro.nextStep.x - uav_enemy_pro.uav.x) + uav_enemy_pro.nextStep.x;
				int ny = (uav_enemy_pro.nextStep.y - uav_enemy_pro.uav.y) + uav_enemy_pro.nextStep.y;
				int nz = (uav_enemy_pro.nextStep.z - uav_enemy_pro.uav.z) + uav_enemy_pro.nextStep.z;
				Point3 nnStep = new Point3(nx, ny, nz);
				checkPointInMap(map, nnStep);
				if (pathA[nnStep.x][nnStep.y] == null) {
					int[][][] path = new int[high.length][map.length][map[0].length];
					Point2 point2 = new Point2(nnStep.x, nnStep.y);
					for (int j = 0; j < high.length; j++) {
						int h = high[j];
						path[j] = ponitPathXY(map[h], point2, 0);
					}
					pathA[nnStep.x][nnStep.y] = path;
				}
			}
		}
	}

	private static void foecastNectStep(char[][][] map, ArrayList<Goods> goods) {
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			UAV_enemy_Pro uav_enemy_pro = uav_enemy_proList.get(i);
			if (uav_enemy_pro.uav.goods_no != -1) {
				for (int j = 0; j < goods.size(); j++) {
					if (goods.get(j).no == uav_enemy_pro.uav.goods_no) {
						uav_enemy_pro.good = goods.get(j);
					}
				}
			} else {
				uav_enemy_pro.good = null;
			}
			Point3 previousStep = uav_enemy_pro.previousStep;
			if (previousStep == null) {
				previousStep = new Point3(uav_enemy_pro.uav.x, uav_enemy_pro.uav.y, uav_enemy_pro.uav.z);
			}
			int nx = (uav_enemy_pro.uav.x - previousStep.x) + uav_enemy_pro.uav.x;
			int ny = (uav_enemy_pro.uav.y - previousStep.y) + uav_enemy_pro.uav.y;
			int nz = (uav_enemy_pro.uav.z - previousStep.z) + uav_enemy_pro.uav.z;
			if (uav_enemy_pro.nextStep != null) {
				uav_enemy_pro.nextStep.x = nx;
				uav_enemy_pro.nextStep.y = ny;
				uav_enemy_pro.nextStep.z = nz;
			} else {
				uav_enemy_pro.nextStep = new Point3(nx, ny, nz);
			}

			if (uav_enemy_pro.good != null && uav_enemy_pro.uav.x == uav_enemy_pro.good.end_x
					&& uav_enemy_pro.uav.y == uav_enemy_pro.good.end_y) {
				uav_enemy_pro.nextStep.x = uav_enemy_pro.uav.x;
				uav_enemy_pro.nextStep.y = uav_enemy_pro.uav.y;
				uav_enemy_pro.nextStep.z = uav_enemy_pro.uav.z - 1;
			}
			checkPointInMap(map, uav_enemy_pro.nextStep);
			if (uav_enemy_pro.uav.status == 2
					&& map[uav_enemy_pro.nextStep.z][uav_enemy_pro.nextStep.x][uav_enemy_pro.nextStep.y] != 'f') {
				uav_enemy_pro.nextStep.x = uav_enemy_pro.uav.x;
				uav_enemy_pro.nextStep.y = uav_enemy_pro.uav.y;
				uav_enemy_pro.nextStep.z = uav_enemy_pro.uav.z;
			}
		}
	}

	private static void checkPointInMap(char[][][] map, Point3 point) {
		if (point.x < 0) {
			point.x = 0;
		}
		if (point.y < 0) {
			point.y = 0;
		}
		if (point.z < 0) {
			point.z = 0;
		}
		if (point.x >= map[0].length) {
			point.x = map[0].length - 1;
		}
		if (point.y >= map[0][0].length) {
			point.y = map[0][0].length - 1;
		}
		if (point.z >= map.length) {
			point.z = map.length - 1;
		}
	}

	private static void stepInit(ArrayList<UAV_Pro> uav_ProList, ArrayList<UAV_price> uav_priceList,
			ArrayList<UAV_we> uav_we) {
		for (int i = 0; i < uav_ProList.size(); i++) {
			int no = uav_ProList.get(i).uav.no;
			boolean b = false;
			for (int j = 0; j < uav_we.size(); j++) {
				int no1 = uav_we.get(j).no;
				if (no == no1 && (uav_we.get(j).status == 0 || uav_we.get(j).status == 3)) {
					b = true;
					uav_ProList.get(i).uav.x = uav_we.get(j).x;
					uav_ProList.get(i).uav.y = uav_we.get(j).y;
					uav_ProList.get(i).uav.z = uav_we.get(j).z;
					uav_ProList.get(i).uav.goods_no = uav_we.get(j).goods_no;
					uav_ProList.get(i).uav.remain_electricity = uav_we.get(j).remain_electricity;
					uav_we.remove(j);
					break;
				}
			}
			if (b == false) {
				uav_ProList.remove(i);
				i--;
			}
		}
		for (int i = 0; i < uav_ProList.size(); i++) {
			if (uav_ProList.get(i).capacity == null) {
				for (int k = 0; k < uav_priceList.size(); k++) {
					UAV_price uav_price = uav_priceList.get(k);
					if (uav_price.type.equals(uav_ProList.get(i).type)) {
						uav_ProList.get(i).capacity = uav_price.capacity;
						uav_ProList.get(i).charge = uav_price.charge;
						break;
					}
				}
			}
		}
		for (int i = 0; i < uav_we.size(); i++) {
			if (uav_we.get(i).status == 0) {
				UAV_Pro uavpro = new UAV_Pro();
				uavpro.uav = uav_we.get(i);
				for (int k = 0; k < uav_priceList.size(); k++) {
					UAV_price uav_price = uav_priceList.get(k);
					if (uav_price.type.equals(uav_we.get(i).type)) {
						uavpro.type = uav_price.type;
						uavpro.value = uav_price.value;
						uavpro.load_weight = uav_price.load_weight;
						uavpro.capacity = uav_price.capacity;
						uavpro.charge = uav_price.charge;
						break;
					}
				}
				uav_ProList.add(uavpro);
			}
		}
	}

	private static void goodproInit(char[][][] map, ArrayList<Goods> goods, int[] high) {
		for (int i = 0; i < goodpros.size(); i++) {
			int no = goodpros.get(i).good.no;
			boolean b = false;
			for (int j = 0; j < goods.size(); j++) {
				int no1 = goods.get(j).no;
				if (no == no1) {
					b = true;
					goodpros.get(i).good = goods.get(j);
					goods.remove(j);
					break;
				}
			}
			if (b == false) {
				goodpros.remove(i);
				i--;
			}
		}
		for (int i = 0; i < goods.size(); i++) {
			if (goods.get(i).status == 0) {
				goodpro goodpro = new goodpro();
				goodpro.good = goods.get(i);
				Goods good = goodpro.good;
				Point3 toPoitnt = new Point3(good.start_x, good.start_y, 0);
				Point3 toPoitnt2 = new Point3(good.end_x, good.end_y, 0);
				int[][][] toPonitPathXYZ2 = ponitPathXYZ(map, toPoitnt2, high);
				int[] cl2 = selectHigh(toPonitPathXYZ2, toPoitnt, toPoitnt2);
				if (cl2[0] == -1) {
					goodpro.distance = 2099999999;
					goodpro.electricity = 2099999999;
					continue;
				}
				goodpro.distance = cl2[1];
				goodpro.electricity = (cl2[1] + 1) * good.weight;
				goodpros.add(goodpro);
			}
		}
	}

	private static void stepInit2(char[][][] map, ArrayList<Goods> goods, ArrayList<UAV_price> uav_priceList,
			ArrayList<UAV_enemy> uav_enemy) {
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			if (uav_enemy_proList.get(i).previousStep == null) {
				uav_enemy_proList.get(i).previousStep = new Point3(uav_enemy_proList.get(i).uav.x,
						uav_enemy_proList.get(i).uav.y, uav_enemy_proList.get(i).uav.z);
			} else {
				uav_enemy_proList.get(i).previousStep.x = uav_enemy_proList.get(i).uav.x;
				uav_enemy_proList.get(i).previousStep.y = uav_enemy_proList.get(i).uav.y;
				uav_enemy_proList.get(i).previousStep.z = uav_enemy_proList.get(i).uav.z;
			}
		}
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			int no = uav_enemy_proList.get(i).uav.no;
			boolean b = false;
			for (int j = 0; j < uav_enemy.size(); j++) {
				int no1 = uav_enemy.get(j).no;
				if (no == no1 && (uav_enemy.get(j).status == 0 || uav_enemy.get(j).status == 2
						|| uav_enemy.get(j).status == 3)) {
					b = true;
					uav_enemy_proList.get(i).uav = uav_enemy.get(j);
					uav_enemy.remove(j);
					break;
				}
			}
			if (b == false) {
				uav_enemy_proList.remove(i);
				i--;
			}
		}
		for (int i = 0; i < uav_enemy.size(); i++) {
			if (uav_enemy.get(i).status == 0 || uav_enemy.get(i).status == 2) {
				UAV_enemy_Pro uav_enemy_pro = new UAV_enemy_Pro();
				uav_enemy_pro.uav = uav_enemy.get(i);
				for (int k = 0; k < uav_priceList.size(); k++) {
					UAV_price uav_price = uav_priceList.get(k);
					if (uav_price.type.equals(uav_enemy.get(i).type)) {
						uav_enemy_pro.type = uav_price.type;
						uav_enemy_pro.value = uav_price.value;
						uav_enemy_pro.load_weight = uav_price.load_weight;
						break;
					}
				}
				uav_enemy_pro.previousStep = new Point3(uav_enemy_pro.uav.x, uav_enemy_pro.uav.y, uav_enemy_pro.uav.z);
				uav_enemy_proList.add(uav_enemy_pro);
			}
		}

		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			UAV_enemy uavenemy = uav_enemy_proList.get(i).uav;
			if (uavenemy.status == 2) {
				if (uav_enemy_proList.get(i).nextStep != null) {
					uav_enemy_proList.get(i).uav.x = uav_enemy_proList.get(i).nextStep.x;
					uav_enemy_proList.get(i).uav.y = uav_enemy_proList.get(i).nextStep.y;
					uav_enemy_proList.get(i).uav.z = uav_enemy_proList.get(i).nextStep.z;
				} else {
					uav_enemy_proList.get(i).uav.x = 0;
					uav_enemy_proList.get(i).uav.y = 0;
					uav_enemy_proList.get(i).uav.z = 0;
				}
			}
		}
		foecastNectStep(map, goods);
	}

	public static void distribtGoods(ArrayList<UAV_Pro> uav_ProList, int[] high, char[][][] map, int time) {
		for (int i = 0; i < uav_ProList.size(); i++) {
			if (uav_ProList.get(i).good != null && uav_ProList.get(i).model == 'g') {
				int no = uav_ProList.get(i).good.no;
				boolean b = false;
				for (int j = 0; j < goodpros.size(); j++) {
					if (no == goodpros.get(j).good.no && goodpros.get(j).good.status == 0
							&& goodpros.get(j).good.left_time > uav_ProList.get(i).uav.z) {
						b = true;
						break;
					}
				}
				if (b == false && uav_ProList.get(i).uav.goods_no != no) {
					uav_ProList.get(i).good = null;
				}
			}
		}
		for (UAV_Pro uavpro : uav_ProList) {
			if (uavpro.good != null && uavpro.model == 'g') {
				if (uavpro.uav.goods_no == -1) {
					uavpro.toPoint = new Point3(uavpro.good.start_x, uavpro.good.start_y, 0);
				} else {
					uavpro.toPoint.x = uavpro.good.end_x;
					uavpro.toPoint.y = uavpro.good.end_y;
					uavpro.toPoint.z = 0;

					boolean be = true, bw = true;
					if (Math.abs(uavpro.uav.x - uavpro.good.end_x) < 3 && Math.abs(uavpro.uav.y - uavpro.good.end_y) < 3
							&& uavpro.uav.z >= h_low) {
						uavpro.canMove = true;
						l1: for (int j = 0; j < uav_enemy_proList.size(); j++) {
							if (Math.abs(uav_enemy_proList.get(j).uav.x - uavpro.good.end_x) <= 1
									&& Math.abs(uav_enemy_proList.get(j).uav.y - uavpro.good.end_y) <= 1
									&& uav_enemy_proList.get(j).uav.z <= h_low + 1
									&& uavpro.uav.remain_electricity > uavpro.good.weight * (h_low + 5)) {
								uavpro.toPoint.x = uavpro.uav.x;
								uavpro.toPoint.y = uavpro.uav.y;
								uavpro.toPoint.z = uavpro.uav.z;
								be = false;

								for (int i = 0; i < uav_ProList.size(); i++) {
									if (uav_ProList.get(i).enemy != null
											&& uav_ProList.get(i).enemy.uav.no == uav_enemy_proList.get(j).uav.no) {
										break l1;
									}
								}
								int[] weuav_VlueSortNum = uav_weValueSortNum(uav_ProList);
								for (int i = 0; i < uav_ProList.size(); i++) {
									UAV_Pro u_pr = uav_ProList.get(weuav_VlueSortNum[i]);
									if (u_pr.good == null && u_pr.enemy == null
											&& u_pr.value <= Math.max(uavpro.value, uav_enemy_proList.get(j).value)) {
										uav_ProList.get(weuav_VlueSortNum[i]).model = 'b';
										uav_ProList.get(weuav_VlueSortNum[i]).enemy = uav_enemy_proList.get(j);
										break l1;
									}
								}
								for (int i = 0; i < uav_ProList.size(); i++) {
									UAV_Pro u_pr = uav_ProList.get(weuav_VlueSortNum[i]);
									if (u_pr.uav.goods_no == -1 && u_pr.enemy == null
											&& u_pr.value <= Math.max(uavpro.value, uav_enemy_proList.get(j).value)) {
										uav_ProList.get(weuav_VlueSortNum[i]).model = 'b';
										uav_ProList.get(weuav_VlueSortNum[i]).enemy = uav_enemy_proList.get(j);
										break l1;
									}
								}
								uavpro.canMove = false;
								uavpro.staticEnemy = uav_enemy_proList.get(j);
								break l1;
							}
						}
					}

					if (uavpro.uav.x == uavpro.good.end_x && uavpro.uav.y == uavpro.good.end_y) {
						for (int j = 0; j < uav_ProList.size(); j++) {
							if (uav_ProList.get(j).uav.x == uavpro.good.end_x
									&& uav_ProList.get(j).uav.y == uavpro.good.end_y
									&& uav_ProList.get(j).uav.z < uavpro.uav.z && uav_ProList.get(j).uav.z < h_low) {
								uavpro.toPoint.z = Math.min(h_high, h_low + 2);
								bw = false;
								break;
							}
						}
					}
					if (!be && !bw) {
						uavpro.toPoint.z = 0;
					}
					boolean b = false;
					for (int i = 0; i < uav_ProList.size(); i++) {
						if (uav_ProList.get(i).canMove == true) {
							b = true;
						}
					}
					if (b == false) {
						if (uav_ProList.size() > 1) {
							if (uav_ProList.get(uav_ProList.size() - 1).good.value > uav_ProList.get(0).good.value) {
								uav_ProList.get(0).toPoint = new Point3(
										uav_ProList.get(uav_ProList.size() - 1).staticEnemy.uav.x,
										uav_ProList.get(uav_ProList.size() - 1).staticEnemy.uav.y,
										uav_ProList.get(uav_ProList.size() - 1).staticEnemy.uav.z);
							} else {
								uav_ProList.get(uav_ProList.size() - 1).toPoint = new Point3(
										uav_ProList.get(0).staticEnemy.uav.x, uav_ProList.get(0).staticEnemy.uav.y,
										uav_ProList.get(0).staticEnemy.uav.z);
							}
						}
					}
				}
			}
			if (uavpro.good == null && uavpro.model == 'g') {
				if (uavpro.uav.remain_electricity == uavpro.capacity) {
					uavpro.needElecticity = false;
				}
				if (uavpro.needElecticity == true) {
					uavpro.toPoint = new Point3(park.x, park.y, 0);
					if (Math.abs(uavpro.uav.x - park.x) < 6 && Math.abs(uavpro.uav.y - park.y) < 6
							&& uavpro.uav.z >= h_low) {
						for (int j = 0; j < uav_ProList.size(); j++) {
							if (uav_ProList.get(j).uav.x == park.x && uav_ProList.get(j).uav.y == park.y
									&& uav_ProList.get(j).uav.z <= h_low && uav_ProList.get(j).uav.no != uavpro.uav.no
									&& uav_ProList.get(j).toPoint != null && uav_ProList.get(j).toPoint.z > 0
									&& uav_ProList.get(j).uav.z > 0) {
								uavpro.toPoint.x = Math.max(uavpro.toPoint.x - 5, 0);
								// if (uavpro.toPoint.x + 5 < map[0].length - 1)
								// {
								// uavpro.toPoint.x = Math.min(uavpro.toPoint.x
								// + 5, map[0].length - 1);
								// }
								uavpro.toPoint.y = Math.max(uavpro.toPoint.y - 5, 0);
								// if (uavpro.toPoint.y + 5 < map[0][0].length -
								// 1) {
								// uavpro.toPoint.y = Math.min(uavpro.toPoint.y
								// + 5, map[0][0].length - 1);
								// }
								uavpro.toPoint.z = Math.min(h_high, h_low + 9);
								break;
							}
						}
					}
				} else {
					if (fogPList.size() > 0) {
						Point3 p = fogPList.get(uavpro.uav.no % fogPList.size());
						uavpro.toPoint = new Point3(p.x, p.y, p.z);
					} else {
						uavpro.toPoint = new Point3(park.getX(), park.getY(), h_high);
						uavpro.toPoint.x = Math.max(uavpro.toPoint.x - 5, 0);
						if (uavpro.toPoint.x + 5 < map[0].length - 1) {
							uavpro.toPoint.x = Math.min(uavpro.toPoint.x + 5, map[0].length - 1);
						}
						uavpro.toPoint.y = Math.max(uavpro.toPoint.y - 5, 0);
						if (uavpro.toPoint.y + 5 < map[0][0].length - 1) {
							uavpro.toPoint.y = Math.min(uavpro.toPoint.y + 5, map[0][0].length - 1);
						}
						uavpro.toPoint.z = Math.min(h_high, h_low + 6);
					}
				}
			}
		}
		for (int i = 0; i < uav_enemy_proList.size(); i++) {
			if (uav_enemy_proList.get(i).good != null && uav_enemy_proList.get(i).uav.goods_no == -1
					&& uav_enemy_proList.get(i).uav.x == uav_enemy_proList.get(i).good.start_x
					&& uav_enemy_proList.get(i).uav.y == uav_enemy_proList.get(i).good.start_y) {
				Point3 toPoitnt = new Point3(uav_enemy_proList.get(i).good.end_x, uav_enemy_proList.get(i).good.end_y,
						0);
				boolean b = true;
				for (UAV_Pro uavpro : uav_ProList) {
					if (uavpro.enemy != null && uavpro.enemy.uav.no == uav_enemy_proList.get(i).uav.no) {
						b = false;
					}
				}
				if (b) {
					for (int k = 0; k < uav_ProList.size(); k++) {
						UAV_Pro uavpro = uav_ProList.get(uav_ProList.size() - 1 - k);
						if (uavpro.good == null && uavpro.model == 'g') {
							Point3 frompointwe = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
							Point3 fromPointenemy0 = new Point3(uav_enemy_proList.get(i).uav.x,
									uav_enemy_proList.get(i).uav.y, uav_enemy_proList.get(i).uav.z);
							int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
							int[] clenemy = selectHigh(toPonitPathXYZ, fromPointenemy0, toPoitnt);
							int[] clwe = selectHigh(toPonitPathXYZ, frompointwe, toPoitnt);
							if (clenemy[1] + 2 * uav_enemy_proList.get(i).uav.z - 2 > clwe[1] + 1
									&& uav_enemy_proList.get(i).value + uav_enemy_proList.get(i).good.value >= 2
											* uavpro.value) {
								uavpro.model = 'b';
								uavpro.enemy = uav_enemy_proList.get(i);
								break;
							}
						} else if (uavpro.uav.z >= h_low && uavpro.uav.goods_no == -1 && uavpro.good != null
								&& uav_enemy_proList.get(i).value + uav_enemy_proList.get(i).good.value >= 2
										* (uavpro.value + uavpro.good.value)) {
							Point3 frompointwe = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
							Point3 fromPointenemy0 = new Point3(uav_enemy_proList.get(i).uav.x,
									uav_enemy_proList.get(i).uav.y, uav_enemy_proList.get(i).uav.z);
							int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
							int[] clenemy = selectHigh(toPonitPathXYZ, fromPointenemy0, toPoitnt);
							int[] clwe = selectHigh(toPonitPathXYZ, frompointwe, toPoitnt);
							if (clenemy[1] + 2 * uav_enemy_proList.get(i).uav.z - 2 > clwe[1] + 1) {
								uavpro.model = 'b';
								uavpro.enemy = uav_enemy_proList.get(i);
								break;
							}
						}
					}
				}
			}
			if (uav_enemy_proList.get(i).uav.goods_no != -1) {
				Point3 toPoitnt = new Point3(uav_enemy_proList.get(i).good.end_x, uav_enemy_proList.get(i).good.end_y,
						0);
				boolean b = true;
				for (UAV_Pro uavpro : uav_ProList) {
					if (uavpro.enemy != null && uavpro.enemy.uav.no == uav_enemy_proList.get(i).uav.no) {
						b = false;
					}
				}
				if (b) {
					for (int k = 0; k < uav_ProList.size(); k++) {
						UAV_Pro uavpro = uav_ProList.get(uav_ProList.size() - 1 - k);
						if (uavpro.good == null && uavpro.model == 'g') {
							Point3 frompointwe = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
							Point3 fromPointenemy0 = new Point3(uav_enemy_proList.get(i).uav.x,
									uav_enemy_proList.get(i).uav.y, uav_enemy_proList.get(i).uav.z);
							int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
							int[] clenemy = selectHigh(toPonitPathXYZ, fromPointenemy0, toPoitnt);
							int[] clwe = selectHigh(toPonitPathXYZ, frompointwe, toPoitnt);
							if (clenemy[1] > clwe[1] + 1 && uav_enemy_proList.get(i).value
									+ uav_enemy_proList.get(i).good.value >= 2 * uavpro.value) {
								uavpro.model = 'b';
								uavpro.enemy = uav_enemy_proList.get(i);
								break;
							}
						} else if (uavpro.uav.z >= h_low && uavpro.uav.goods_no == -1 && uavpro.good != null
								&& uav_enemy_proList.get(i).value + uav_enemy_proList.get(i).good.value >= 2
										* (uavpro.value + uavpro.good.value)) {
							Point3 frompointwe = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
							Point3 fromPointenemy0 = new Point3(uav_enemy_proList.get(i).uav.x,
									uav_enemy_proList.get(i).uav.y, uav_enemy_proList.get(i).uav.z);
							int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
							int[] clenemy = selectHigh(toPonitPathXYZ, fromPointenemy0, toPoitnt);
							int[] clwe = selectHigh(toPonitPathXYZ, frompointwe, toPoitnt);
							if (clenemy[1] > clwe[1] + 1) {
								uavpro.model = 'b';
								uavpro.enemy = uav_enemy_proList.get(i);
								break;
							}
						}
					}
				}
			}
		}
	}

	private static void getGood(ArrayList<UAV_Pro> uav_ProList, int[] high, char[][][] map, int time) {
		for (UAV_Pro uav : uav_ProList) {
			if (uav.uav.goods_no == -1) {
				uav.good = null;
			} else {
				uav.model = 'g';
				uav.enemy = null;
			}
		}

		for (int e = 0; e < uav_ProList.size(); e++) {
			UAV_Pro uavpro = uav_ProList.get(e);
			if (uavpro.uav.goods_no == -1 && uavpro.model == 'g' && uavpro.needElecticity == false) {
				uavpro.goodlist = uav_goodList(high, map, time, uavpro);
				if (uavpro.goodlist.size() > 0) {
					goodsLongSortNum(uavpro);
				}
			}
		}
		float[] vla = new float[uav_ProList.size()];
		for (int i = 0; i < vla.length; i++) {
			vla[i] = uav_ProList.get(i).maxVL;
		}
		int[] vlsortNum = sortNum(vla);
		for (int e = 0; e < uav_ProList.size(); e++) {
			UAV_Pro uavpro = uav_ProList.get(vlsortNum[e]);
			if (uavpro.uav.goods_no == -1 && uavpro.model == 'g' && uavpro.needElecticity == false) {
				ArrayList<int[]> gs = uavpro.goodlist;
				if (gs.size() > 0) {
					int[] goodSortNum = uavpro.goodSortNum;
					// if (uavpro.uav.no <= 5) {
					// System.out.println("///////////////////////");
					// for (int i = 0; i < gs.size(); i++) {
					// float val = gs.get(goodSortNum[i])[1] + 10;
					// float lon = gs.get(goodSortNum[i])[2] + 10;
					// Goods good =
					// goodpros.get(gs.get(goodSortNum[i])[0]).good;
					// System.out.println(goodpros.get(gs.get(goodSortNum[i])[0]).good.no
					// + " l:"
					// + gs.get(goodSortNum[i])[2] + " v:" +
					// gs.get(goodSortNum[i])[1] + " "
					// + lon * lon * lon / val + " " + "v/l:"
					// + gs.get(goodSortNum[i])[1] * 1.0 /
					// gs.get(goodSortNum[i])[2] * 1.0);
					// }
					// }

					l1: for (int i = 0; i < gs.size(); i++) {
						int index = goodpros.get(gs.get(goodSortNum[i])[0]).good.no;
						for (int j = 0; j < uav_ProList.size(); j++) {
							if (uav_ProList.get(j).good != null && uav_ProList.get(j).good.no == index) {
								continue l1;
							}
						}
						for (int j = 0; j < uav_enemy_proList.size(); j++) {
							if (uav_enemy_proList.get(j).good != null && uav_enemy_proList.get(j).good.no == index) {
								Point3 fromPointenemy0 = new Point3(uav_enemy_proList.get(j).previousStep.x,
										uav_enemy_proList.get(j).previousStep.y,
										uav_enemy_proList.get(j).previousStep.z);
								Point3 fromPointenemy1 = new Point3(uav_enemy_proList.get(j).uav.x,
										uav_enemy_proList.get(j).uav.y, uav_enemy_proList.get(j).uav.z);
								Point3 toPoitnt = new Point3(uav_enemy_proList.get(j).good.start_x,
										uav_enemy_proList.get(j).good.start_y, 0);
								int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
								int[] clenemy0 = selectHigh(toPonitPathXYZ, fromPointenemy0, toPoitnt);
								int[] clenemy1 = selectHigh(toPonitPathXYZ, fromPointenemy1, toPoitnt);
								if (clenemy0[1] > clenemy1[1]) {
									Point3 fromPointwe = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
									int[] clwe = selectHigh(toPonitPathXYZ, fromPointwe, toPoitnt);
									if (clenemy1[1] < clwe[1] && i < gs.size() - 1) {
										continue l1;
									}
								}

							}
						}
						uavpro.good = goodpros.get(gs.get(goodSortNum[i])[0]).good;
						if (Math.abs(uavpro.uav.x - uavpro.good.start_x) < 5
								&& Math.abs(uavpro.uav.y - uavpro.good.start_y) < 5) {
							for (int j = 0; j < uav_enemy_proList.size(); j++) {
								if (uav_enemy_proList.get(j).nextStep.x == uavpro.good.start_x
										&& uav_enemy_proList.get(j).nextStep.y == uavpro.good.start_y
										&& uav_enemy_proList.get(j).nextStep.z < uavpro.uav.z
										&& uav_enemy_proList.get(j).value < uavpro.value) {
									uavpro.good = null;
									continue l1;
								}
							}
						}
						if (goodpros.get(gs.get(goodSortNum[i])[0]).electricity >= uavpro.uav.remain_electricity) {
							if (goodpros.get(gs.get(goodSortNum[i])[0]).electricity < uavpro.capacity
									&& i >= (goodSortNum.length - 1) / 2) {
								uavpro.needElecticity = true;
								uavpro.good = null;
								break l1;
							} else {
								uavpro.good = null;
								continue l1;
							}
						}
						uavpro.toPoint = new Point3(uavpro.good.start_x, uavpro.good.start_y, 0);
						if (uavpro.uav.x == uavpro.good.start_x && uavpro.uav.y == uavpro.good.start_y) {
							for (int j = 0; j < uav_ProList.size(); j++) {
								if (uav_ProList.get(j).uav.x == uavpro.good.start_x
										&& uav_ProList.get(j).uav.y == uavpro.good.start_y
										&& uav_ProList.get(j).uav.z < uavpro.uav.z
										&& uav_ProList.get(j).uav.z < h_low) {
									uavpro.toPoint.z = Math.min(h_high, h_low + 2);
									break;
								}
							}
						}
						break l1;
					}
				}
			}
		}
	}

	private static void enemy_getGood(int[] high, char[][][] map, int time) {
		for (UAV_enemy_Pro uav : uav_enemy_proList) {
			if (uav.uav.goods_no == -1) {
				uav.good = null;
			}
		}

		for (int e = 0; e < uav_enemy_proList.size(); e++) {
			UAV_enemy_Pro uavpro = uav_enemy_proList.get(e);
			if (uavpro.uav.goods_no == -1) {
				uavpro.goodlist = enemy_goodList(high, map, time, uavpro);
				if (uavpro.goodlist.size() > 0) {
					enemy_goodsLongSortNum(uavpro);
				}
			}
		}
		float[] vla = new float[uav_enemy_proList.size()];
		for (int i = 0; i < vla.length; i++) {
			vla[i] = uav_enemy_proList.get(i).maxVL;
		}
		int[] vlsortNum = sortNum(vla);
		for (int e = 0; e < uav_enemy_proList.size(); e++) {
			UAV_enemy_Pro uavpro = uav_enemy_proList.get(vlsortNum[e]);
			if (uavpro.uav.goods_no == -1) {
				ArrayList<int[]> gs = uavpro.goodlist;
				if (gs.size() > 0) {
					int[] goodSortNum = uavpro.goodSortNum;
					// if (uavpro.uav.no <= 5) {
					// System.out.println("///////////////////////");
					// for (int i = 0; i < gs.size(); i++) {
					// float val = gs.get(goodSortNum[i])[1] + 10;
					// float lon = gs.get(goodSortNum[i])[2] + 10;
					// Goods good =
					// goodpros.get(gs.get(goodSortNum[i])[0]).good;
					// System.out.println(goodpros.get(gs.get(goodSortNum[i])[0]).good.no
					// + " l:"
					// + gs.get(goodSortNum[i])[2] + " v:" +
					// gs.get(goodSortNum[i])[1] + " "
					// + lon * lon * lon / val + " " + "v/l:"
					// + gs.get(goodSortNum[i])[1] * 1.0 /
					// gs.get(goodSortNum[i])[2] * 1.0);
					// }
					// }

					l1: for (int i = 0; i < gs.size(); i++) {
						int index = goodpros.get(gs.get(goodSortNum[i])[0]).good.no;
						for (int j = 0; j < uav_enemy_proList.size(); j++) {
							if (uav_enemy_proList.get(j).good != null && uav_enemy_proList.get(j).good.no == index) {
								continue l1;
							}
						}
						uavpro.good = goodpros.get(gs.get(goodSortNum[i])[0]).good;

						if (goodpros.get(gs.get(goodSortNum[i])[0]).electricity >= uavpro.uav.remain_electricity) {
							uavpro.good = null;
							continue l1;
						}
						uavpro.toPoint = new Point3(uavpro.good.start_x, uavpro.good.start_y, 0);
						break l1;
					}
				}
			}
		}
	}

	public static void distribtEnemy(ArrayList<UAV_Pro> uav_ProList, int[] high, char[][][] map) {
		for (UAV_Pro uavpro : uav_ProList) {
			if (uavpro.enemy != null && uavpro.model == 'b' && uav_enemy_proList.size() > 0) {
				boolean b = false;
				for (int i = 0; i < uav_enemy_proList.size(); i++) {
					if (uav_enemy_proList.get(i).uav.no == uavpro.enemy.uav.no) {
						b = true;
						break;
					}
				}
				if (b == false) {
					uavpro.enemy = null;
				}
			}
		}
		for (UAV_Pro uavpro : uav_ProList) {
			if (uavpro.enemy == null && uavpro.model == 'b' && uav_enemy_proList.size() > 0) {
				int[] vSortN = uav_enemyValueSortNum(uav_enemy_proList);
				l1: for (int i = 0; i < uav_enemy_proList.size(); i++) {
					int enemyno = uav_enemy_proList.get(vSortN[vSortN.length - 1 - i]).uav.no;
					for (int j = 0; j < uav_ProList.size(); j++) {
						if (uav_ProList.get(j).enemy != null && uav_ProList.get(j).enemy.uav.no == enemyno) {
							continue l1;
						}
					}
					uavpro.enemy = uav_enemy_proList.get(vSortN[vSortN.length - 1 - i]);
					break;
				}
				// if (uavpro.enemy != null && we_value - uavpro.value <
				// enemy_value - uavpro.enemy.value) {
				// uavpro.enemy = null;
				// }
			}
		}
		boolean bat = true;
		if (uav_enemy_proList.size() <= 1) {
			int valueWe = 0;
			int valueEnemy = 0;
			for (int i = 0; i < uav_ProList.size(); i++) {
				if (uav_ProList.get(i).good != null) {
					valueWe += uav_ProList.get(i).good.value;
				}
			}
			for (int i = 0; i < uav_enemy_proList.size(); i++) {
				if (uav_enemy_proList.get(i).uav.goods_no != -1 && uav_enemy_proList.get(i).good != null) {
					valueEnemy = uav_enemy_proList.get(i).good.value;
				}
			}
			if (valueWe > valueEnemy + valueEnemy / 3 && uav_ProList.size() - uav_enemy_proList.size() > 2) {
				bat = false;
			}
		}
		if (bat) {
			for (UAV_Pro uavpro : uav_ProList) {
				if (uavpro.model == 'b') {
					if (uavpro.enemy != null) {
						if (uavpro.enemy.uav.goods_no == -1) {
							if (uavpro.toPoint == null) {
								uavpro.toPoint = new Point3(uavpro.enemy.nextStep.x, uavpro.enemy.nextStep.y,
										uavpro.enemy.nextStep.z);
							} else {
								uavpro.toPoint.x = uavpro.enemy.nextStep.x;
								uavpro.toPoint.y = uavpro.enemy.nextStep.y;
								uavpro.toPoint.z = uavpro.enemy.nextStep.z;
							}

						} else {
							uavpro.toPoint = new Point3(uavpro.enemy.good.end_x, uavpro.enemy.good.end_y, h_low);
							if (Math.abs(uavpro.uav.x - uavpro.enemy.good.end_x) <= 1
									&& Math.abs(uavpro.uav.y - uavpro.enemy.good.end_y) <= 1
									&& Math.abs(uavpro.uav.z - h_low) <= 1) {
								boolean b = true;
								int x = uavpro.enemy.good.end_x;
								int y = uavpro.enemy.good.end_y;

								if (x - 1 >= 0 && map[h_low][x - 1][y] == 'f') {
									b = false;
								}
								if (y - 1 >= 0 && map[h_low][x][y - 1] == 'f') {
									b = false;
								}
								if (x + 1 < map[0].length && map[h_low][x + 1][y] == 'f') {
									b = false;
								}
								if (y + 1 < map[0][0].length && map[h_low][x][y + 1] == 'f') {
									b = false;
								}

								if (x - 1 >= 0 && y - 1 >= 0 && map[h_low][x - 1][y - 1] == 'f') {
									b = false;
								}
								if (x - 1 >= 0 && y + 1 < map[0][0].length && map[h_low][x - 1][y + 1] == 'f') {
									b = false;
								}
								if (x + 1 < map[0].length && y + 1 < map[0][0].length
										&& map[h_low][x + 1][y + 1] == 'f') {
									b = false;
								}
								if (x + 1 < map[0].length && y - 1 >= 0 && map[h_low][x + 1][y - 1] == 'f') {
									b = false;
								}
								if (b) {
									if (h_low + 1 < map.length && map[h_low + 1][x][y] == 'f') {
										if (h_low + 1 <= h_high) {
											uavpro.toPoint.z = h_low + 1;
										}
									} else {
										if (x - 1 >= 0 && y - 1 >= 0 && map[h_low][x - 1][y - 1] != 'b') {
											uavpro.toPoint.x = x - 1;
											uavpro.toPoint.y = y - 1;
										} else if (x - 1 >= 0 && y + 1 < map[0][0].length
												&& map[h_low][x - 1][y + 1] != 'b') {
											uavpro.toPoint.x = x - 1;
											uavpro.toPoint.y = y + 1;
										} else if (x + 1 < map[0].length && y + 1 < map[0][0].length
												&& map[h_low][x + 1][y + 1] != 'b') {
											uavpro.toPoint.x = x + 1;
											uavpro.toPoint.y = y + 1;
										} else if (x + 1 < map[0].length && y - 1 >= 0
												&& map[h_low][x + 1][y - 1] != 'b') {
											uavpro.toPoint.x = x + 1;
											uavpro.toPoint.y = y - 1;
										} else if (x - 1 >= 0 && map[h_low][x - 1][y] != 'b') {
											uavpro.toPoint.x = x - 1;
										} else if (y - 1 >= 0 && map[h_low][x][y - 1] != 'b') {
											uavpro.toPoint.y = y - 1;
										} else if (x + 1 < map[0].length && map[h_low][x + 1][y] != 'b') {
											uavpro.toPoint.x = x + 1;
										} else if (y + 1 < map[0][0].length && map[h_low][x][y + 1] != 'b') {
											uavpro.toPoint.y = y + 1;
										}
									}
									if (Math.abs(uavpro.enemy.uav.x - uavpro.enemy.good.end_x) <= 1
											&& Math.abs(uavpro.enemy.uav.y - uavpro.enemy.good.end_y) <= 1
											&& Math.abs(uavpro.enemy.uav.z - h_low) <= 1) {
										uavpro.toPoint.x = x;
										uavpro.toPoint.y = y;
										uavpro.toPoint.z = h_low;
									}
								}
							}

							if (uavpro.enemy.uav.status == 0 && uavpro.enemy.uav.z < h_low
									&& uavpro.enemy.uav.x == uavpro.enemy.good.start_x
									&& uavpro.enemy.uav.y == uavpro.enemy.good.start_y) {

								if (uavpro.uav.z <= h_low && uavpro.uav.x == uavpro.enemy.good.start_x
										&& uavpro.uav.y == uavpro.enemy.good.start_y) {
									uavpro.toPoint = new Point3(uavpro.enemy.nextStep.x, uavpro.enemy.nextStep.y,
											uavpro.enemy.nextStep.z);
								} else {
									Point3 fromPoint = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
									Point3 toPoitnt = new Point3(uavpro.enemy.good.start_x, uavpro.enemy.good.start_y,
											h_low);
									int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
									int[] cl = selectHigh(toPonitPathXYZ, fromPoint, toPoitnt);
									if (cl[1] < h_low - uavpro.enemy.uav.z) {
										uavpro.toPoint = new Point3(uavpro.enemy.nextStep.x, uavpro.enemy.nextStep.y,
												uavpro.enemy.nextStep.z);
									}
								}
							}
						}
					} else {
						uavpro.model = 'g';
					}
				}
			}
		}
	}

	private static int[] uav_enemyValueSortNum(ArrayList<UAV_enemy_Pro> uav_enemy_proList2) {
		int[] value = new int[uav_enemy_proList2.size()];
		for (int i = 0; i < uav_enemy_proList2.size(); i++) {
			value[i] = uav_enemy_proList2.get(i).value;
		}
		int[] valueSortNum = sortNum(value);
		return valueSortNum;
	}

	private static int[] uav_weValueSortNum(ArrayList<UAV_Pro> uav_we_proList) {
		int[] value = new int[uav_we_proList.size()];
		for (int i = 0; i < uav_we_proList.size(); i++) {
			value[i] = uav_we_proList.get(i).value;
		}
		int[] valueSortNum = sortNum(value);
		return valueSortNum;
	}

	private static int[] uav_weZSortNum(ArrayList<UAV_Pro> uav_we_proList) {
		int[] value = new int[uav_we_proList.size()];
		for (int i = 0; i < uav_we_proList.size(); i++) {
			value[i] = uav_we_proList.get(i).uav.z * 5 + uav_we_proList.get(i).uav.y * 2 + uav_we_proList.get(i).uav.x;
			if (uav_we_proList.get(i).uav.goods_no != -1) {
				value[i] -= 10;
			}
		}
		int[] valueSortNum = sortNum(value);
		return valueSortNum;
	}

	private static int[] uav_weZSortNum0(ArrayList<UAV_Pro> uav_we_proList) {
		int[] z = new int[uav_we_proList.size()];
		for (int i = 0; i < uav_we_proList.size(); i++) {
			z[i] = uav_we_proList.get(i).uav.z;
		}
		int[] zSortNum = sortNum(z);
		return zSortNum;
	}

	public static void distributeNextStep(ArrayList<UAV_Pro> uav_ProList, int[] htoc, char[][][] map, int[] high) {
		for (int i = 0; i < uav_ProList.size(); i++) {
			nextStepListXYZ(uav_ProList.get(i), htoc, map, high, 1);
			uav_ProList.get(i).nextStep = null;
		}
		for (int i = 0; i < uav_ProList.size(); i++) {
			UAV_Pro uavpro = uav_ProList.get(i);
			if (uavpro.model == 'g') {
				for (int j = 0; j < uavpro.nextStepList.size(); j++) {
					for (int e = 0; e < uav_enemy_proList.size(); e++) {
						if (uavpro.nextStepList.size() > 1 && uavpro.value > uav_enemy_proList.get(e).value
								&& uav_enemy_proList.get(e).nextStep.equals(uavpro.nextStepList.get(j))) {
							uavpro.nextStepList.remove(j);
							j--;
						}
					}
				}
			}
		}
		int[] zSortNum = uav_weZSortNum(uav_ProList);
//		for (int i = 0; i < uav_ProList.size(); i++) {
//
//			UAV_Pro uavpro = uav_ProList.get(zSortNum[i]);
//			System.out.print(uavpro.uav.no+" ");
//			System.out.print(uavpro.toPoint+" ");
//			for (int j = 0; j < uavpro.nextStepList.size(); j++) {
//				System.out.print(uavpro.nextStepList.get(j) + " ");
//			}
//			System.out.println();
//		}
		for (int i = 0; i < uav_ProList.size(); i++) {

			UAV_Pro uavpro = uav_ProList.get(zSortNum[i]);

			distributeStep0(uav_ProList, uavpro);
			// for (int j = 0; j < uav_ProList.size(); j++) {
			// System.out.println(uav_ProList.get(j).nextStep);
			// }
			// System.out.println();
		}

		for (int i = 0; i < uav_ProList.size(); i++) {
			UAV_Pro uavpro = uav_ProList.get(i);
			if (uavpro.nextStep == null) {
				uavpro.nextStep = new Point3(uavpro.uav.x, uavpro.uav.y, uavpro.uav.z);
			}
		}
	}

	private static void distributeStep0(ArrayList<UAV_Pro> uav_ProList, UAV_Pro uavpro) {
		System.out.println(uavpro.uav.no);
		if (uavpro.uav.x == park.x && uavpro.uav.y == park.y && uavpro.uav.z == 0) {
			for (int r = 0; r < uav_ProList.size(); r++) {
				if (uav_ProList.get(r).needElecticity == true && uav_ProList.get(r).uav.x == park.x
						&& uav_ProList.get(r).uav.y == park.y && uav_ProList.get(r).uav.z < h_low
						&& uav_ProList.get(r).uav.z > 0) {
					uavpro.nextStep = new Point3(park.x, park.y, 0);
				}
			}
		}
		if (uavpro.nextStep != null) {
			return;
		}
		for (int j = 0; j < uavpro.nextStepList.size(); j++) {
			Point3 point = uavpro.nextStepList.get(j);
			int x0 = uavpro.uav.x;
			int y0 = uavpro.uav.y;
			int z0 = uavpro.uav.z;
			int x1 = point.x;
			int y1 = point.y;
			int z1 = point.z;
			int no1 = uavpro.uav.no;
			boolean b = true;
			for (int k = 0; k < uav_ProList.size(); k++) {
				int kx0 = uav_ProList.get(k).uav.x;
				int ky0 = uav_ProList.get(k).uav.y;
				int kz0 = uav_ProList.get(k).uav.z;
				int kno0 = uav_ProList.get(k).uav.no;
				if (x1 == kx0 && y1 == ky0 && z1 == kz0 && no1 != kno0) {
					uavpro.nextStep = new Point3(x1, y1, z1);
					System.out.println("转到");
					if (x1 == park.x && y1 == park.y && z1 == 0) {
						
					}else{
						distributeStep0(uav_ProList, uav_ProList.get(k));
					}
//					System.out.println("///////");
					uavpro.nextStep = null;
				}
			}
			for (int k = 0; k < uav_ProList.size(); k++) {
				int kno0 = uav_ProList.get(k).uav.no;
				if (uav_ProList.get(k).nextStep != null && no1 != kno0) {
					int kx0 = uav_ProList.get(k).uav.x;
					int ky0 = uav_ProList.get(k).uav.y;
					int kz0 = uav_ProList.get(k).uav.z;
					int kx1 = uav_ProList.get(k).nextStep.x;
					int ky1 = uav_ProList.get(k).nextStep.y;
					int kz1 = uav_ProList.get(k).nextStep.z;
					if (x1 == kx1 && y1 == ky1 && z1 == kz1) {
						b = false;
						if (x1 == park.x && y1 == park.y && z1 == 0) {
							b = true;
						}
					}

					if (x0 + x1 == kx0 + kx1 && y0 + y1 == ky0 + ky1 && z0 + z1 == kz0 + kz1) {
						b = false;
					}
				}
			}
			if (b) {
				uavpro.nextStep = point;
				uavpro.nextStepList.clear();
				uavpro.nextStepList.add(point);
				if (uavpro.good != null) {
					if (uavpro.uav.goods_no == -1) {
						if (uavpro.nextStep.x == uavpro.good.start_x && uavpro.nextStep.y == uavpro.good.start_y
								&& uavpro.nextStep.z == 0) {
							uavpro.uav.goods_no = uavpro.good.no;
						}
					}
					if (uavpro.uav.goods_no != -1) {
						if (uavpro.nextStep.x == uavpro.good.end_x && uavpro.nextStep.y == uavpro.good.end_y
								&& uavpro.nextStep.z == 0) {
							// uavpro.good = null;
							uavpro.toPoint.z = Math.min(h_low + 1, h_high);
						}
					}
				}
				break;
			}
		}
	}

	// 初始化参数
	public static char[][][] initialization(JSONObject jsonObject, Parking park, int[][] h_low_high_bildingHight,
			ArrayList<UAV_price> UAV_priceList, ArrayList<UAV_Pro> uavList) {
		JSONObject mapSize = (JSONObject) jsonObject.get("map");
		int mapx = (Integer) mapSize.get("x");
		int mapy = (Integer) mapSize.get("y");
		int mapz = (Integer) mapSize.get("z");
		char[][][] map = new char[mapz][mapx][mapy];
		map[0][0][0] = 'w';
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				Arrays.fill(map[i][j], 'w');
			}
		}
		JSONObject parking = (JSONObject) jsonObject.get("parking");
		int parkingx = (Integer) parking.get("x");
		int parkingy = (Integer) parking.get("y");
		park.setX(parkingx);
		park.setY(parkingy);
		int[] h_low_high = { 1, 2 };
		h_low_high[0] = (Integer) jsonObject.get("h_low");
		h_low_high[1] = (Integer) jsonObject.get("h_high");
		h_low_high_bildingHight[0] = h_low_high;

		JSONArray fog = (JSONArray) jsonObject.get("fog");
		for (int i = 0; i < fog.size(); i++) {
			JSONObject fog0 = fog.getJSONObject(i);
			int fogx = (Integer) fog0.get("x");
			int fogy = (Integer) fog0.get("y");
			int fogl = (Integer) fog0.get("l");
			int fogw = (Integer) fog0.get("w");
			int fogb = (Integer) fog0.get("b");
			int fogt = (Integer) fog0.get("t");
			for (int j = fogb; j < fogt; j++) {
				for (int k = fogy; k < fogw + fogy; k++) {
					for (int e = fogx; e < fogl + fogx; e++) {
						map[j][e][k] = 'f';
					}
				}
			}
		}
		JSONArray building = (JSONArray) jsonObject.get("building");

		int[] bildingHight = new int[building.size()];
		for (int i = 0; i < building.size(); i++) {
			JSONObject building0 = building.getJSONObject(i);
			int buildingx = (Integer) building0.get("x");
			int buildingy = (Integer) building0.get("y");
			int buildingl = (Integer) building0.get("l");
			int buildingw = (Integer) building0.get("w");
			int buildingh = (Integer) building0.get("h");
			bildingHight[i] = buildingh;
			for (int j = 0; j < buildingh; j++) {
				for (int k = buildingy; k < buildingw + buildingy; k++) {
					for (int e = buildingx; e < buildingl + buildingx; e++) {
						map[j][e][k] = 'b';
					}
				}
			}
		}
		int h_low = h_low_high[0];
		int h_high = h_low_high[1];
		bildingHight = noReAndsortAndBtwen(bildingHight, h_low, h_high);
		int[] htoc = htoc(bildingHight, h_high);
		h_low_high_bildingHight[1] = bildingHight;

		// formatSyOut(map[59]);

		JSONArray UAV_price = (JSONArray) jsonObject.get("UAV_price");
		for (int i = 0; i < UAV_price.size(); i++) {
			UAV_price jsonObj = JSONObject.parseObject(UAV_price.get(i).toString(), UAV_price.class);
			UAV_priceList.add(jsonObj);
		}

		JSONArray init_UAV = (JSONArray) jsonObject.get("init_UAV");
		for (int i = 0; i < init_UAV.size(); i++) {
			UAV_Pro uavpro = new UAV_Pro();
			Init_UAV jsonObj = JSONObject.parseObject(init_UAV.get(i).toString(), Init_UAV.class);
			UAV_we uav_we = new UAV_we(jsonObj.no, jsonObj.type, jsonObj.x, jsonObj.y, jsonObj.z, jsonObj.goods_no,
					jsonObj.status, jsonObj.remain_electricity);
			uavpro.uav = uav_we;
			for (int k = 0; k < UAV_priceList.size(); k++) {
				UAV_price uav_price = UAV_priceList.get(k);

				if (uav_price.type.equals(uav_we.type)) {
					uavpro.type = uav_price.type;
					uavpro.value = uav_price.value;
					uavpro.load_weight = uav_price.load_weight;
					break;
				}
			}
			uavpro.load_weight = jsonObj.load_weight;
			uavList.add(uavpro);
		}

		return map;
	}

	private static int[] noReAndsortAndBtwen(int[] bildingHight0, int h_low, int h_high) {
		ArrayList<Integer> bildingHight = new ArrayList<Integer>();
		Arrays.sort(bildingHight0);
		if (bildingHight0[0] >= h_low) {
			bildingHight.add(bildingHight0[0]);
		}
		for (int i = 1; i < bildingHight0.length; i++) {
			int a = bildingHight0[i];
			if (a != bildingHight0[i - 1] && a >= h_low && a <= h_high) {
				bildingHight.add(bildingHight0[i]);
			}
		}
		if (bildingHight.get(0) != h_low) {
			bildingHight.add(0, h_low);
		}
		int[] a = new int[bildingHight.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = bildingHight.get(i);
		}
		return a;
	}

	public static int distibute(ArrayList<int[]> gs) {
		float[] value = new float[gs.size()];
		for (int j = 0; j < gs.size(); j++) {
			float val = gs.get(j)[1];
			float lon = gs.get(j)[2];
			value[j] = val;
			if (lon != 0) {
				value[j] = val / lon;
			}
		}
		int indx = -1;
		int val = -1;
		for (int i = 0; i < value.length; i++) {
			if (value[i] > val) {
				indx = i;
			}
		}
		return indx;
	}

	public static void goodsLongSortNum(UAV_Pro uavpro) {
		ArrayList<int[]> gs = uavpro.goodlist;
		float[] value = new float[gs.size()];
		for (int j = 0; j < gs.size(); j++) {
			float val = gs.get(j)[1] + 10;
			float lon = gs.get(j)[2] + 10;
			value[j] = val;
			if (val != 0) {
				value[j] = lon * lon / val;
			}
		}
		uavpro.goodSortNum = sortNum(value);
		uavpro.maxVL = value[0];
	}

	public static void enemy_goodsLongSortNum(UAV_enemy_Pro uavpro) {
		ArrayList<int[]> gs = uavpro.goodlist;
		float[] value = new float[gs.size()];
		for (int j = 0; j < gs.size(); j++) {
			float val = gs.get(j)[1] + 5;
			float lon = gs.get(j)[2] + 5;
			value[j] = val;
			if (val != 0) {
				value[j] = lon / val;
			}
		}
		uavpro.goodSortNum = sortNum(value);
		uavpro.maxVL = value[0];
	}

	private static ArrayList<int[]> uav_goodList(int[] high, char[][][] map, int time, UAV_Pro uavpro) {
		Point3 fromPoint = new Point3(uavpro.nextStep.x, uavpro.nextStep.y, uavpro.nextStep.z);
		ArrayList<int[]> gs = new ArrayList<int[]>();
		for (int i = 0; i < goodpros.size(); i++) {
			Goods good = goodpros.get(i).good;
			if (good.status != 0) {
				continue;
			}
			if (good.weight > uavpro.load_weight) {
				continue;
			}
			Point3 toPoitnt = new Point3(good.start_x, good.start_y, 0);
			int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);
			int[] cl = selectHigh(toPonitPathXYZ, fromPoint, toPoitnt);
			if (cl[0] == -1) {
				continue;
			}
			if (cl[1] >= good.left_time) {
				continue;
			}
			int val = good.value;
			if (goodpros.get(i).distance >= 1999999) {
				continue;
			}
			int[] uav_good = { i, val, cl[1] + goodpros.get(i).distance };
			gs.add(uav_good);
		}
		return gs;
	}

	private static ArrayList<int[]> enemy_goodList(int[] high, char[][][] map, int time, UAV_enemy_Pro uavEnemyPro) {
		Point3 fromPoint = new Point3(uavEnemyPro.uav.x, uavEnemyPro.uav.y, uavEnemyPro.uav.z);
		ArrayList<int[]> gs = new ArrayList<int[]>();
		for (int i = 0; i < goodpros.size(); i++) {
			Goods good = goodpros.get(i).good;
			if (good.status != 0) {
				continue;
			}
			if (good.weight > uavEnemyPro.load_weight) {
				continue;
			}
			Point3 toPoitnt = new Point3(good.start_x, good.start_y, 0);
			int[][][] toPonitPathXYZ = ponitPathXYZ(map, toPoitnt, high);

			int[] cl = selectHigh(toPonitPathXYZ, fromPoint, toPoitnt);

			if (cl[0] == -1) {
				continue;
			}
			if (cl[1] >= good.left_time) {
				continue;
			}
			int val = good.value;

			if (goodpros.get(i).distance >= 1999999) {
				continue;
			}
			int[] uav_good = { i, val, cl[1] + goodpros.get(i).distance };
			gs.add(uav_good);
		}
		return gs;
	}

	private static void sendStep(int uavValueSum, int uav_enemyValueSum, ArrayList<UAV_Pro> uav_ProList,
			ArrayList<UAV_price> uav_priceList, int[] UAV_priceSotNum, PrintWriter printWriter, String token)
			throws IOException {
		SendObj sendobj = new SendObj();
		sendobj.action = "flyPlane";
		sendobj.token = token;
		ArrayList<UAV_info> uavinfList = new ArrayList<UAV_info>();
		for (int i = 0; i < uav_ProList.size(); i++) {
			UAV_info uavinfo = new UAV_info();
			uavinfo.goods_no = uav_ProList.get(i).uav.goods_no;
			uavinfo.no = uav_ProList.get(i).uav.no;
			uavinfo.x = uav_ProList.get(i).nextStep.x;
			uavinfo.y = uav_ProList.get(i).nextStep.y;
			uavinfo.z = uav_ProList.get(i).nextStep.z;
			uavinfo.remain_electricity = uav_ProList.get(i).uav.remain_electricity;
			uavinfList.add(uavinfo);
		}
		int money = we_value;
		ArrayList<Purchase_UAV> purchase_UAVList = new ArrayList<Purchase_UAV>();
		if (uav_ProList.size() < uav_enemy_proList.size() + 19 && money > uav_priceList.get(UAV_priceSotNum[0]).value
				&& uav_ProList.size() < 150) {
			int load = 0;
			if (uav_ProList.size() - uav_enemy_proList.size() > 16 && uavValueSum > uav_enemyValueSum
					&& goodpros.size() > 1) {
				int[] goodsWeight = new int[goodpros.size()];
				for (int i = 0; i < goodpros.size(); i++) {
					goodsWeight[i] = goodpros.get(i).good.weight;
				}
				int[] gWSortNum = sortNum(goodsWeight);
				load = goodpros.get(gWSortNum[(goodpros.size() - 1) / 2]).good.weight;
			}
			for (int i = 0; i < uav_priceList.size(); i++) {
				int pricNum = UAV_priceSotNum[i];
				if (uav_priceList.get(pricNum).load_weight >= load && uav_priceList.get(pricNum).value <= money) {
					money -= uav_priceList.get(pricNum).value;
					Purchase_UAV puchUav = new Purchase_UAV();
					puchUav.purchase = uav_priceList.get(pricNum).type;
					purchase_UAVList.add(puchUav);
					break;
				}
			}
		}
		sendobj.UAV_info = uavinfList;
		sendobj.purchase_UAV = purchase_UAVList;
		sendData(sendobj, printWriter);
	}

	private static void nextStepListXYZ(UAV_Pro uavpro, int[] htoc, char[][][] map, int[] high, int k) {
		if (uavpro.toPoint == null) {
			uavpro.toPoint = new Point3(uavpro.uav.x, uavpro.uav.y, Math.min(h_high, map.length));
		}
		if (uavpro.toPoint != null) {
			UAV_we n1 = uavpro.uav;
			int x = n1.x;
			int y = n1.y;
			int z = n1.z;
			ArrayList<Point3> nextPointList = new ArrayList<Point3>();
			if (z >= h_low) {
				int[][][] path3 = ponitPathXYZ(map, uavpro.toPoint, high);
				nextPointList = nextPointListXY(path3, x, y, z, htoc, 1);
				if (uavpro.toPoint.x == x && uavpro.toPoint.y == y && uavpro.toPoint.z == z) {
					nextPointList.add(0, new Point3(x, y, z));
				}
				ArrayList<Point3> nextPointList0 = nextPointListXY(path3, x, y, z, htoc, 0);
				ArrayList<Point3> nextPointList1 = nextPointListXY(path3, x, y, z, htoc, -1);
				int l = path3[htoc[z]][x][y] + Math.abs(z - uavpro.toPoint.z);
				if (z == h_low) {
					if (map[z - 1][x][y] != 'b') {
						if (uavpro.toPoint.x == uavpro.uav.x && uavpro.toPoint.y == uavpro.uav.y
								&& uavpro.toPoint.z < h_low) {
							nextPointList.add(new Point3(x, y, z - 1));
						} else {
							nextPointList1.add(new Point3(x, y, z - 1));
						}
					}
				}
				if (z > h_low && z > 0 && map[z - 1][x][y] != 'b') {
					int z0 = z - 1;
					addNectPoint(uavpro, htoc, x, y, z, path3, nextPointList, nextPointList0, nextPointList1, l, z0);
				}
				if (z < h_high && map[z + 1][x][y] != 'b') {
					int z0 = z + 1;
					addNectPointH(uavpro, htoc, x, y, z, path3, nextPointList, nextPointList0, nextPointList1, l, z0);
				}
				for (int i = 0; i < nextPointList0.size(); i++) {
					nextPointList.add(nextPointList0.get(i));
				}
				for (int i = 0; i < nextPointList1.size(); i++) {
					nextPointList.add(nextPointList1.get(i));
				}
				uavpro.nextStepList = nextPointList;
			} else {
				if (uavpro.toPoint.x == x && uavpro.toPoint.y == y && uavpro.toPoint.z == z) {
					nextPointList.add(0, new Point3(x, y, z));
				}
				if (uavpro.toPoint.x == x && uavpro.toPoint.y == y && uavpro.toPoint.z < z) {
					if (z - 1 >= 0 && map[z - 1][x][y] != 'b') {
						nextPointList.add(new Point3(x, y, z - 1));
					}
					if (map[z + 1][x][y] != 'b') {
						nextPointList.add(new Point3(x, y, z + 1));
					}
				} else {
					if (map[z + 1][x][y] != 'b') {
						nextPointList.add(new Point3(x, y, z + 1));
					}
					if (z - 1 >= 0 && map[z - 1][x][y] != 'b') {
						nextPointList.add(new Point3(x, y, z - 1));
					}
				}
				uavpro.nextStepList = nextPointList;
			}
		}
	}

	private static void addNectPoint(UAV_Pro uavpro, int[] htoc, int x, int y, int z, int[][][] path3,
			ArrayList<Point3> nextPointList, ArrayList<Point3> nextPointList0, ArrayList<Point3> nextPointList1, int l,
			int z0) {
		int c = htoc[z0];
		int l0 = path3[c][x][y] + Math.abs(z0 - uavpro.toPoint.z) + Math.abs(z0 - z);
		if (path3[c][x][y] >= 0 && l0 <= l - 1) {
			nextPointList.add(new Point3(x, y, z0));
		} else if (path3[c][x][y] >= 0 && l0 == l) {
			nextPointList0.add(new Point3(x, y, z0));
		} else if (path3[c][x][y] >= 0 && l0 == l + 1) {
			nextPointList1.add(new Point3(x, y, z0));
		}
	}

	private static void addNectPointH(UAV_Pro uavpro, int[] htoc, int x, int y, int z, int[][][] path3,
			ArrayList<Point3> nextPointList, ArrayList<Point3> nextPointList0, ArrayList<Point3> nextPointList1, int l,
			int z0) {
		int c = htoc[z0];
		int l0 = path3[c][x][y] + Math.abs(z0 - uavpro.toPoint.z) + Math.abs(z0 - z);
		for (int i = z0; i <= h_high; i++) {
			if (path3[htoc[i]][x][y] >= 0) {
				c = htoc[i];
				int l1 = path3[c][x][y] + Math.abs(i - uavpro.toPoint.z) + Math.abs(i - z);
				l0 = Math.min(l1, l0);
			}
		}
		if (path3[c][x][y] >= 0 && l0 <= l - 1) {
			nextPointList.add(0, new Point3(x, y, z0));
		} else if (path3[c][x][y] >= 0 && l0 == l) {
			nextPointList0.add(0, new Point3(x, y, z0));
		} else if (path3[c][x][y] >= 0 && l0 == l + 1) {
			nextPointList1.add(0, new Point3(x, y, z0));
		}
	}

	private static ArrayList<Point3> nextPointListXY(int[][][] path3, int x, int y, int z, int[] htoc, int k) {
		ArrayList<Point3> wayList = new ArrayList<Point3>();
		if (z >= h_low && z <= h_high) {
			int c = htoc[z];
			int[][] path = path3[c];
			int l = path[x][y] - k;
			if (time % 2 == 0) {
				if (x - 1 >= 0 && y + 1 < path[0].length && path[x - 1][y + 1] >= 0 && path[x - 1][y + 1] == l) {
					wayList.add(new Point3(x - 1, y + 1, z));
				}
				if (y + 1 < path[0].length && path[x][y + 1] >= 0 && path[x][y + 1] == l) {
					wayList.add(new Point3(x, y + 1, z));
				}
				if (x + 1 < path.length && y + 1 < path[0].length && path[x + 1][y + 1] >= 0
						&& path[x + 1][y + 1] == l) {
					wayList.add(new Point3(x + 1, y + 1, z));
				}
				if (x + 1 < path.length && path[x + 1][y] >= 0 && path[x + 1][y] == l) {
					wayList.add(new Point3(x + 1, y, z));
				}
				if (x - 1 >= 0 && path[x - 1][y] >= 0 && path[x - 1][y] == l) {
					wayList.add(new Point3(x - 1, y, z));
				}
				if (x - 1 >= 0 && y - 1 >= 0 && path[x - 1][y - 1] >= 0 && path[x - 1][y - 1] == l) {
					wayList.add(new Point3(x - 1, y - 1, z));
				}
				if (y - 1 >= 0 && path[x][y - 1] >= 0 && path[x][y - 1] == l) {
					wayList.add(new Point3(x, y - 1, z));
				}
				if (x + 1 < path.length && y - 1 >= 0 && path[x + 1][y - 1] >= 0 && path[x + 1][y - 1] == l) {
					wayList.add(new Point3(x + 1, y - 1, z));
				}
			} else {
				if (y - 1 >= 0 && path[x][y - 1] >= 0 && path[x][y - 1] == l) {
					wayList.add(new Point3(x, y - 1, z));
				}
				if (x + 1 < path.length && y - 1 >= 0 && path[x + 1][y - 1] >= 0 && path[x + 1][y - 1] == l) {
					wayList.add(new Point3(x + 1, y - 1, z));
				}
				if (x - 1 >= 0 && path[x - 1][y] >= 0 && path[x - 1][y] == l) {
					wayList.add(new Point3(x - 1, y, z));
				}
				if (x - 1 >= 0 && y - 1 >= 0 && path[x - 1][y - 1] >= 0 && path[x - 1][y - 1] == l) {
					wayList.add(new Point3(x - 1, y - 1, z));
				}

				if (x + 1 < path.length && y + 1 < path[0].length && path[x + 1][y + 1] >= 0
						&& path[x + 1][y + 1] == l) {
					wayList.add(new Point3(x + 1, y + 1, z));
				}
				if (x + 1 < path.length && path[x + 1][y] >= 0 && path[x + 1][y] == l) {
					wayList.add(new Point3(x + 1, y, z));
				}
				if (x - 1 >= 0 && y + 1 < path[0].length && path[x - 1][y + 1] >= 0 && path[x - 1][y + 1] == l) {
					wayList.add(new Point3(x - 1, y + 1, z));
				}
				if (y + 1 < path[0].length && path[x][y + 1] >= 0 && path[x][y + 1] == l) {
					wayList.add(new Point3(x, y + 1, z));
				}

			}

		}
		return wayList;
	}

	private static int[] htoc(int[] high, int h_high) {
		int[] htoc = new int[h_high + 1];
		for (int i = 0; i < high[0]; i++) {
			htoc[i] = -1;
		}
		for (int i = 0; i < high.length - 1; i++) {
			for (int j = high[i]; j < high[i + 1]; j++) {
				htoc[j] = i;
			}
		}
		for (int j = high[high.length - 1]; j <= h_high; j++) {
			htoc[j] = high.length - 1;
		}
		return htoc;
	}

	private static void formatSyOut(int[][] a) {
		for (int[] is : a) {
			for (int i : is) {
				System.out.printf(" %3d", i);
			}
			System.out.println();
		}
	}

	private static void formatSyOut(char[][] a) {
		for (char[] is : a) {
			for (int i : is) {
				System.out.printf(" %3s", i);
			}
			System.out.println();
		}
	}

	// 一个点到二维地图上任何一点的最短路径的长度阵列
	public static int[][] ponitPathXY(char[][] map, Point2 point, int high) {
		// map[][]中 0表示没有阻碍物，1表示有阻碍物
		// 地图上只有标识符是0的点可以走
		int[][] path = new int[map.length][map[0].length];
		for (int[] is : path) {
			Arrays.fill(is, -1);
		}
		path[point.x][point.y] = high;
		LinkedList<Point2> pointsList = new LinkedList<Point2>();
		LinkedList<Point2> pointsList2 = new LinkedList<Point2>();
		LinkedList<Point2> pointsList3 = new LinkedList<Point2>();
		pointsList.add(point);
		while (true) {
			for (Point2 point2 : pointsList) {
				int x = point2.x;
				int y = point2.y;
				int pathLong = path[x][y];
				if (x - 1 >= 0 && y - 1 >= 0 && map[x - 1][y - 1] != 'b') {
					if (path[x - 1][y - 1] > path[x][y] + 1 || path[x - 1][y - 1] == -1) {
						path[x - 1][y - 1] = path[x][y] + 1;
						pointsList2.add(new Point2(x - 1, y - 1));
					}
				}
				if (x - 1 >= 0 && map[x - 1][y] != 'b') {
					if (path[x - 1][y] > path[x][y] + 1 || path[x - 1][y] == -1) {
						path[x - 1][y] = path[x][y] + 1;
						pointsList2.add(new Point2(x - 1, y));
					}
				}
				if (y - 1 >= 0 && map[x][y - 1] != 'b') {
					if (path[x][y - 1] > path[x][y] + 1 || path[x][y - 1] == -1) {
						path[x][y - 1] = path[x][y] + 1;
						pointsList2.add(new Point2(x, y - 1));
					}
				}
				if (x + 1 < map.length && y + 1 < map[0].length && map[x + 1][y + 1] != 'b') {
					if (path[x + 1][y + 1] > path[x][y] + 1 || path[x + 1][y + 1] == -1) {
						path[x + 1][y + 1] = path[x][y] + 1;
						pointsList2.add(new Point2(x + 1, y + 1));
					}
				}
				if (x + 1 < map.length && map[x + 1][y] != 'b') {
					if (path[x + 1][y] > path[x][y] + 1 || path[x + 1][y] == -1) {
						path[x + 1][y] = path[x][y] + 1;
						pointsList2.add(new Point2(x + 1, y));
					}
				}
				if (y + 1 < map[0].length && map[x][y + 1] != 'b') {
					if (path[x][y + 1] > path[x][y] + 1 || path[x][y + 1] == -1) {
						path[x][y + 1] = path[x][y] + 1;
						pointsList2.add(new Point2(x, y + 1));
					}
				}
				if (x - 1 >= 0 && y + 1 < map[0].length && map[x - 1][y + 1] != 'b') {
					if (path[x - 1][y + 1] > path[x][y] + 1 || path[x - 1][y + 1] == -1) {
						path[x - 1][y + 1] = path[x][y] + 1;
						pointsList2.add(new Point2(x - 1, y + 1));
					}
				}
				if (x + 1 < map.length && y - 1 >= 0 && map[x + 1][y - 1] != 'b') {
					if (path[x + 1][y - 1] > path[x][y] + 1 || path[x + 1][y - 1] == -1) {
						path[x + 1][y - 1] = path[x][y] + 1;
						pointsList2.add(new Point2(x + 1, y - 1));
					}
				}
			}
			if (pointsList2.size() == 0) {
				break;
			} else {
				pointsList3 = pointsList2;
				pointsList2 = pointsList;
				pointsList = pointsList3;
				pointsList2.clear();
			}
		}
		if (map[point.x][point.y] == 'b') {
			path[point.x][point.y] = -1;
		}
		return path;
	}

	public static int[][][] ponitPathXYZ(char[][][] map, Point3 point3, int[] high) {

		if (pathA[point3.x][point3.y] == null) {
			int[][][] path = new int[high.length][map.length][map[0].length];
			Point2 point2 = new Point2(point3.x, point3.y);
			for (int i = 0; i < high.length; i++) {
				int h = high[i];
				path[i] = ponitPathXY(map[h], point2, 0);
			}
			// pathAll.get(point3.x).set(point3.y, path);
			pathA[point3.x][point3.y] = path;
			return path;
		} else {
			int[][][] path = pathA[point3.x][point3.y];
			return path;
		}
	}

	public static int[] selectHigh(int[][][] path, Point3 fromPoint, Point3 toPoitnt) {
		int x = fromPoint.x;
		int y = fromPoint.y;
		int c = -1;
		int l = 2099999999;
		if (toPoitnt.x == fromPoint.x && toPoitnt.y == fromPoint.y) {
			c = 0;
			l = Math.abs(toPoitnt.z - fromPoint.z);
		} else {
			for (int i = 0; i < path.length; i++) {
				int h = h_low;
				for (int j = 0; j < htoc.length; j++) {
					if (htoc[j] == i) {
						h = j;
					}
				}
				int l0 = path[i][x][y] + Math.abs(toPoitnt.z - h) + Math.abs(h - fromPoint.z);
				if (path[i][x][y] >= 0 && l0 <= l) {
					c = i;
					l = l0;
				}
			}
		}
		int[] cl = { c, l };
		return cl;
	}

	public void test2() {
		int[] a = { 2, 3, 9, 8, 8, 8 };
		int[] valueSortNum = sortNum(a);
		System.out.println(a.toString());
		System.out.println(valueSortNum.toString());

	}

	public static int[] sortNum(int[] a) {
		int[] asor = new int[a.length];
		for (int i = 0; i < asor.length; i++) {
			asor[i] = i;
		}
		fastSortNum(a, asor, 0, a.length - 1);
		return asor;
	}

	public static int[] sortNum(float[] a) {
		int[] asor = new int[a.length];
		for (int i = 0; i < asor.length; i++) {
			asor[i] = i;
		}
		fastSortNum(a, asor, 0, a.length - 1);
		return asor;
	}

	public static void fastSortNum(int arr[], int arrsor[], int low, int high) {
		int l = low;
		int h = high;
		int povit = arr[low];
		while (l < h) {
			while (l < h && arr[h] >= povit) {
				h--;
			}

			while (l < h && arr[l] <= povit) {
				l++;
			}
			if (l < h) {
				int temp = arr[h];
				arr[h] = arr[l];
				arr[l] = temp;
				temp = arrsor[h];
				arrsor[h] = arrsor[l];
				arrsor[l] = temp;
			}
		}
		int temp = arr[low];
		arr[low] = arr[l];
		arr[l] = temp;
		temp = arrsor[low];
		arrsor[low] = arrsor[l];
		arrsor[l] = temp;
		if (l > low) {
			fastSortNum(arr, arrsor, low, l - 1);
		}
		if (h < high) {
			fastSortNum(arr, arrsor, l + 1, high);
		}
	}

	public static void fastSortNum(float arr[], int arrsor[], int low, int high) {
		int l = low;
		int h = high;
		float povit = arr[low];
		while (l < h) {
			while (l < h && arr[h] >= povit) {
				h--;
			}

			while (l < h && arr[l] <= povit) {
				l++;
			}
			if (l < h) {
				float temp = arr[h];
				arr[h] = arr[l];
				arr[l] = temp;
				int tmp = arrsor[h];
				arrsor[h] = arrsor[l];
				arrsor[l] = tmp;
			}
		}
		float temp = arr[low];
		arr[low] = arr[l];
		arr[l] = temp;
		int tmp = arrsor[low];
		arrsor[low] = arrsor[l];
		arrsor[l] = tmp;
		if (l > low) {
			fastSortNum(arr, arrsor, low, l - 1);
		}
		if (h < high) {
			fastSortNum(arr, arrsor, l + 1, high);
		}
	}

}

class Point2 {
	int x;
	int y;

	public Point2(int i, int j) {
		this.x = i;
		this.y = j;
	}

	@Override
	public String toString() {
		return "[" + x + "," + y + "]";
	}

}

class Point3 {
	int x;
	int y;
	int z;

	public Point3(int i, int j, int z) {
		this.x = i;
		this.y = j;
		this.z = z;
	}

	@Override
	public String toString() {
		return "[" + x + "," + y + "," + z + "]";
	}
}

class Map {
	private MapSize map;
	private Parking parking;

	public MapSize getMap() {
		return map;
	}

	public void setMap(MapSize map) {
		this.map = map;
	}

	public Parking getParking() {
		return parking;
	}

	public void setParking(Parking parking) {
		this.parking = parking;
	}
}

class MapSize {
	private Integer x;
	private Integer y;
	private Integer z;

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getZ() {
		return z;
	}

	public void setZ(Integer z) {
		this.z = z;
	}
}

class Parking {
	Integer x;
	Integer y;

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

}

class Building {
	public Integer x;
	public Integer y;
	public Integer l;
	public Integer w;
	public Integer h;

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getL() {
		return l;
	}

	public void setL(Integer l) {
		this.l = l;
	}

	public Integer getW() {
		return w;
	}

	public void setW(Integer w) {
		this.w = w;
	}

	public Integer getH() {
		return h;
	}

	public void setH(Integer h) {
		this.h = h;
	}

}

class Fog {
	public Integer x;
	public Integer y;
	public Integer l;
	public Integer w;
	public Integer b;
	public Integer t;

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getL() {
		return l;
	}

	public void setL(Integer l) {
		this.l = l;
	}

	public Integer getW() {
		return w;
	}

	public void setW(Integer w) {
		this.w = w;
	}

	public Integer getB() {
		return b;
	}

	public void setB(Integer b) {
		this.b = b;
	}

	public Integer getT() {
		return t;
	}

	public void setT(Integer t) {
		this.t = t;
	}

}

class goodpro {
	int electricity;
	int distance;
	Goods good;

	@Override
	public String toString() {
		return "goodpro [electricity=" + electricity + ", distance=" + distance + ", good=" + good + "]";
	}

}

class UAV_Pro {
	UAV_we uav;
	Point3 toPoint;
	ArrayList<Point3> nextStepList;
	String type;
	int load_weight;
	int value;
	Point3 nextStep;
	Goods good;
	char model = 'g';
	UAV_enemy_Pro enemy;
	boolean canMove = true;
	boolean needElecticity = true;
	public Integer capacity;
	public Integer charge;
	UAV_enemy_Pro staticEnemy;
	ArrayList<int[]> goodlist;
	int[] goodSortNum;
	float maxVL = 0;

	@Override
	public String toString() {
		return "UAV_Pro [uav=" + uav + ", toPoint=" + toPoint + ", nextPointList=" + nextStepList + ", type=" + type
				+ ", load_weight=" + load_weight + ", value=" + value + ", nextPoint=" + nextStep + ", good=" + good
				+ "]";
	}

}

class UAV_enemy_Pro {
	UAV_enemy uav;
	Point3 toPoint;
	ArrayList<Point3> nextStepList;
	String type;
	int load_weight;
	int value;
	Point3 nextStep;
	Goods good;
	Point3 previousStep;
	ArrayList<int[]> goodlist;
	int[] goodSortNum;
	float maxVL = 0;

	@Override
	public String toString() {
		return "UAV_enemy_Pro [uav=" + uav + ", toPoint=" + toPoint + ", nextStepList=" + nextStepList + ", type="
				+ type + ", load_weight=" + load_weight + ", value=" + value + ", nextStep=" + nextStep + ", good="
				+ good + ", previousStep=" + previousStep + "]";
	}

}

class Init_UAV {
	public Integer no;
	public Integer x;
	public Integer y;
	public Integer z;
	public Integer load_weight;
	public String type;
	public Integer status;
	public Integer goods_no;
	public Integer remain_electricity;

	public Integer getRemain_electricity() {
		return remain_electricity;
	}

	public void setRemain_electricity(Integer remain_electricity) {
		this.remain_electricity = remain_electricity;
	}

	public Integer getNo() {
		return no;
	}

	public void setNo(Integer no) {
		this.no = no;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getZ() {
		return z;
	}

	public void setZ(Integer z) {
		this.z = z;
	}

	public Integer getLoad_weight() {
		return load_weight;
	}

	public void setLoad_weight(Integer load_weight) {
		this.load_weight = load_weight;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getGoods_no() {
		return goods_no;
	}

	public void setGoods_no(Integer goods_no) {
		this.goods_no = goods_no;
	}

}

class UAV_price {
	public String type;
	public Integer load_weight;
	public Integer value;
	public Integer capacity;
	public Integer charge;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getLoad_weight() {
		return load_weight;
	}

	public void setLoad_weight(Integer load_weight) {
		this.load_weight = load_weight;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public Integer getCharge() {
		return charge;
	}

	public void setCharge(Integer charge) {
		this.charge = charge;
	}

}
