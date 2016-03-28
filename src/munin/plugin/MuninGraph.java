package munin.plugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Muninグラフの基本クラス.
 *
 */
public class MuninGraph {
	
	/**
	 * 折れ線情報の名称キー . 
	 */
	public static final String LINE_INFO_NAME = "name";
	
	/**
	 * 折れ線情報のラベルキー .
	 */
	public static final String LINE_INFO_LABEL = "label";
	
	/**
	 * 折れ線情報の値キー .
	 */
	public static final String LINE_INFO_VALUE = "value";
	

	/**
	 * 折れ線情報の描画キー .
	 */
	public static final String LINE_INFO_DRAW = "draw";
	
	/**
	 * 折れ線情報のタイプキー .
	 */
	public static final String LINE_INFO_TYPE = "type";


	/**
	 * 折れ線情報の最大値キー .
	 */
	public static final String LINE_INFO_MAX = "max";

	/**
	 * 折れ線情報の最小値キー .
	 */
	public static final String LINE_INFO_MIN = "min";

	
	/**
	 * グラフのタイトル.
	 */
	private String title = null;
	/**
	 * グラフのargs.
	 */
	private String args = null;
	/**
	 * 縦軸ラベル.
	 */
	private String vlabel = null;
	/**
	 * カテゴリー.
	 */
	private String category = null;
	
	
	/**
	 * トータル設定 .
	 */
	private String total = null;
	
	
	/**
	 * 表示順 .
	 */
	private String order = null;
	
	/**
	 * グラフのライン情報のリスト .
	 */
	private List<HashMap<String, String>> lineInfo = null;

	/**
	 * タイトルを取得する .
	 * @return タイトル.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * タイトルを設定する .
	 * @param title タイトル.
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * グラフの引数を取得する .
	 * @return グラフの引数.
	 */
	public String getArgs() {
		return args;
	}

	/**
	 * グラフの引数を設定する .
	 * @param args グラフの引数.
	 */
	public void setArgs(final String args) {
		this.args = args;
	}

	/**
	 * グラフの縦軸ラベルを取得する .
	 * @return グラフの縦軸ラベル.
	 */
	public String getVlabel() {
		return vlabel;
	}

	/**
	 * グラフの縦軸ラベルを設定する .
	 * @param vlabel グラフの縦軸ラベル.
	 */
	public void setVlabel(final String vlabel) {
		this.vlabel = vlabel;
	}

	/**
	 * グラフのカテゴリーを取得する .
	 * @return グラフのカテゴリー.
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * グラフのカテゴリーを設定する .
	 * @param category グラフのカテゴリー.
	 */
	public void setCategory(final String category) {
		this.category = category;
	}

	/**
	 * Totalを取得する .
	 * @return トータル.
	 */
	public String getTotal() {
		return total;
	}

	/**
	 * Totalを設定する .
	 * @param total トータル.
	 */
	public void setTotal(final String total) {
		this.total = total;
	}


	/**
	 * 表示順を取得する .
	 * @return 表示順.
	 */
	public String getOrder() {
		return order;
	}

	/**
	 * 表示順を設定する .
	 * @param order 表示順.
	 */
	public void setOrder(final String order) {
		this.order = order;
	}

	/**
	 * グラフの折れ線情報を取得する .
	 * @return グラフの折れ線情報.
	 */
	public List<HashMap<String, String>> getLineInfo() {
		return lineInfo;
	}

	/**
	 * グラフの折れ線情報を取得する .
	 * @param lineInfo グラフの折れ線情報.
	 */
	public void setLineInfo(final List<HashMap<String, String>> lineInfo) {
		this.lineInfo = lineInfo;
	}
	
	
	
	
	/**
	 * コンストラクタ .
	 * @param title グラフタイトル.
	 * @param category グラフカテゴリー.
	 */
	public MuninGraph(final String title, final String category) {
		this.setTitle(title);
		this.setCategory(category);
	}
	
	/**
	 * Config情報の出力 .
	 * @return Config情報の文字列..
	 */
	public String getConfigInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph_title "); sb.append(this.getTitle()); sb.append("\n");
        sb.append("graph_args "); sb.append(this.getArgs()); sb.append("\n");
        sb.append("graph_vlabel "); sb.append(this.getVlabel()); sb.append("\n");
        sb.append("graph_category "); sb.append(this.getCategory()); sb.append("\n");
        if (this.total != null) {
            sb.append("graph_total "); sb.append(this.getTotal()); sb.append("\n");
        }
        if (this.order != null) {
            sb.append("graph_order "); sb.append(this.getOrder()); sb.append("\n");
        }
        for (HashMap<String, String> m : this.lineInfo) {
        	String name = m.get(LINE_INFO_NAME);
        	Iterator<String> it = m.keySet().iterator();
        	while (it.hasNext()) {
        		String key = it.next();
        		if (LINE_INFO_NAME.equals(key) || LINE_INFO_VALUE.equals(key)) {
        			// nameとvalueはスキップ.
        			continue;
        		}
        		sb.append(name); sb.append("."); sb.append(key); sb.append(" "); sb.append(m.get(key)); sb.append("\n");
        	}
        }
        return sb.toString();

	}
	
	/**
	 * 値情報の出力 .
	 * @return 値情報の文字列..
	 */
	public String getValueInfo() {
        StringBuilder sb = new StringBuilder();
        for (HashMap<String, String> m : this.lineInfo) {
        	String name = m.get(LINE_INFO_NAME);
        	Iterator<String> it = m.keySet().iterator();
        	while (it.hasNext()) {
        		String key = it.next();
        		if (!LINE_INFO_VALUE.equals(key)) {
        			// value以外はスキップ.
        			continue;
        		}
        		sb.append(name); sb.append("."); sb.append(key); sb.append(" "); sb.append(m.get(key)); sb.append("\n");
        	}
        }
        return sb.toString();

	}

}
