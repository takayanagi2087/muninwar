package munin.plugin;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Wrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.modeler.Registry;

/**
 * Munin plugin webapp for Tomcat.
 *
 */
public class MuninPluginServlet extends HttpServlet implements ContainerServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = -2039491315143718725L;

	/**
	 * グラフカテゴリー.
	 */
	private static final String GRAPH_CATEGORY = "Catalina";

	/**
	 * Log.
	 */
	private static Log log = LogFactory.getLog(MuninPluginServlet.class);

	/**
     * The Wrapper container associated with this servlet.
     */
    private transient Wrapper wrapper = null;

    /**
     * The associated host.
     */
    private transient Host host = null;
    /**
     * The Context container associated with our web application.
     */
    private transient Context context = null;

    /**
     * MBeanServer.
     */
    private MBeanServer mBeanServer = null;

    /**
     * Vector of thread pools object names.
     */
    private Vector<ObjectName> threadPools = new Vector<ObjectName>();


    /**
     * Vector of global request processors object names.
     */
    private Vector<ObjectName> globalRequestProcessors = new Vector<ObjectName>();

    /**
     * スレッドプールを取得する .
     * @throws Exception 例外.
     */
    private void getThreadPools() throws Exception {
        // Query Thread Pools
        String onStr = "*:type=ThreadPool,*";
        ObjectName objectName;
		objectName = new ObjectName(onStr);
        Set<ObjectInstance> set  = mBeanServer.queryMBeans(objectName, null);
        Iterator<ObjectInstance> iterator = set.iterator();
        while (iterator.hasNext()) {
            ObjectInstance oi = iterator.next();
            threadPools.addElement(oi.getObjectName());
        }
    }

    /**
     * GlobalRequestProcessorsを取得する .
     * @throws Exception 例外.
     */
    private void getGlobalRequestProcessors() throws Exception {

        // Query Global Request Processors
        String onStr = "*:type=GlobalRequestProcessor,*";
        ObjectName objectName = new ObjectName(onStr);
        Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
        Iterator<ObjectInstance> iterator = set.iterator();
        while (iterator.hasNext()) {
            ObjectInstance oi = iterator.next();
            globalRequestProcessors.addElement(oi.getObjectName());
        }
    }




    /**
     * 初期化処理 .
     *
     * @throws ServletException サーブレット例外.
     *
     */
    @Override
    public void init() throws ServletException {
    	super.init();
    	this.mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
		try {
			this.getThreadPools();
			this.getGlobalRequestProcessors();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

    }

    /**
     * Warpperを設定する .
     * @param wrapper Warapper .
     */
	@Override
	public void setWrapper(final Wrapper wrapper) {
		this.wrapper = wrapper;
		if (this.wrapper == null) {
			this.context = null;
			this.host = null;
			log.warn("no wrapper");
		} else {
            this.context = (Context) wrapper.getParent();
            this.host = (Host) this.context.getParent();
			log.info("wrapper name = " + wrapper.getName() + ":" + wrapper.getStateName());
		}
	}

	/**
	 * Wrapperを取得する .
	 * @return Wrapper.
	 */
	@Override
	public Wrapper getWrapper() {
		return this.wrapper;
	}

	/**
	 * 出力指定されたコンテキストかどうかをチェックする.
	 * @param contextName コンテキスト名.
	 * @param contextList 出力コンテキストリスト.
	 * @return 出力対象の場合true.
	 */
	private boolean isOutputContext(final String contextName, final String[] contextList) {
		boolean ret = true;
		if (contextList != null) {
			if (contextList.length > 0) {
				int matchcount = 0;
				for (String c : contextList) {
					if (contextName.indexOf(c) >= 0) {
						matchcount++;
						break;
					}
				}
				if (matchcount == 0) {
					ret = false;
				}
			}
		}
		return ret;
	}

	/**
	 * 各アプリケーション後とのセッション数をレポートするグラフを作成する .
	 * @param opt オプション (必要に応じて、出力するContextの指定をする).
	 * @return 作成したグラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getSessionsGraph(final String opt) throws Exception {
		MuninGraph graph = new MuninGraph("Tomcat webapp sessions", GRAPH_CATEGORY);
		graph.setArgs("-l 0 --base 1000");
		graph.setVlabel("sessions");
		graph.setTotal("total");

		String [] contextList = null;
		if (opt != null) {
			if (opt.length() > 0) {
				contextList = opt.split(" ");
			}
		}

		Container[] children = host.findChildren();
        String[] contextNames = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            contextNames[i] = children[i].getName();
        }
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
        for (String contextName : contextNames) {
        	if (contextName.length() == 0) {
        		continue;
        	}
        	if (this.isOutputContext(contextName, contextList)) {
            	Context ctxt = (Context) host.findChild(contextName);
                int sessions = ctxt.getManager().getActiveSessions();
            	HashMap<String, String> lineinfo = new HashMap<String, String>();
            	lineinfo.put(MuninGraph.LINE_INFO_NAME, contextName.substring(1));
            	lineinfo.put(MuninGraph.LINE_INFO_LABEL, contextName);
            	lineinfo.put(MuninGraph.LINE_INFO_VALUE, Integer.toString(sessions));
            	lines.add(lineinfo);
        	}
        }
        graph.setLineInfo(lines);

		return graph;
	}

	/**
	 * Thread Poolを取得する .
	 * @param opt スレッドプール指定オプション.
	 * @return スレッドプール.
	 */
	private ObjectName getThreadPool(final String opt) {
		String poolname = "8009";
		if (opt != null) {
			if (opt.length() > 0) {
				poolname = opt;
			}
		}
		ObjectName threadpool = null;
		for (ObjectName on : this.threadPools) {
			String tpName = on.getKeyProperty("name");
			log.info(tpName + ":" + poolname);
			if (tpName.indexOf(poolname) >= 0) {
				threadpool = on;
				break;
			}
		}
		return threadpool;
	}

	/**
	 * Global request processorを取得する .
	 * @param name 対応するスレッドプール名称.
	 * @return Global request processor.
	 */
	private ObjectName getGlobalRequestProcessor(final String name) {
		ObjectName grpName = null;
		Enumeration<ObjectName> enumeration = globalRequestProcessors.elements();
	    while (enumeration.hasMoreElements()) {
	        ObjectName objectName = enumeration.nextElement();
	        if (name.equals(objectName.getKeyProperty("name"))) {
	            grpName = objectName;
	        }
	    }
	    return grpName;
    }


	/**
	 * スレッド数グラフを取得する .
	 * @param opt コネクタ名を指定する.
	 * <pre>
	 * コネクタ名を指定しない場合、8009を含むコネクタを表示する.
	 * </pre>
	 * @return 作成したグラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getThreadsGraph(final String opt) throws Exception {
		ObjectName threadpool = getThreadPool(opt);
		if (threadpool == null) {
			return null;
		}
		String tpName = threadpool.getKeyProperty("name");
		MuninGraph graph = new MuninGraph("Tomcat threads of " + tpName, GRAPH_CATEGORY);
		graph.setArgs("--base 1000 -l 0");
		graph.setVlabel("threads");
		graph.setOrder("busy idle");

		String current = mBeanServer.getAttribute(threadpool, "currentThreadCount").toString();
		String busy = mBeanServer.getAttribute(threadpool, "currentThreadsBusy").toString();
		int idle = Integer.parseInt(current) - Integer.parseInt(busy);
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
        {
    		HashMap<String, String> line = new HashMap<String, String>();
    		line.put(MuninGraph.LINE_INFO_NAME, "busy");
    		line.put(MuninGraph.LINE_INFO_LABEL, "busy");
    		line.put(MuninGraph.LINE_INFO_VALUE, busy);
    		line.put(MuninGraph.LINE_INFO_DRAW, "AREA");
    		lines.add(line);
        }
        {
    		HashMap<String, String> line = new HashMap<String, String>();
    		line.put(MuninGraph.LINE_INFO_NAME, "idle");
    		line.put(MuninGraph.LINE_INFO_LABEL, "idle");
    		line.put(MuninGraph.LINE_INFO_VALUE, Integer.toString(idle));
    		line.put(MuninGraph.LINE_INFO_DRAW, "STACK");
    		lines.add(line);

        }

        graph.setLineInfo(lines);
		return graph;
	}


	/**
	 * 転送量グラフを取得する .
	 * @param opt コネクタ名を指定する.
	 * <pre>
	 * コネクタ名を指定しない場合、8009を含むコネクタを表示する.
	 * </pre>
	 * @return 作成したグラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getVolumeGraph(final String opt) throws Exception {
		ObjectName threadpool = getThreadPool(opt);
		if (threadpool == null) {
			return null;
		}
		String tpName = threadpool.getKeyProperty("name");
		String name = threadpool.getKeyProperty("name");
		ObjectName grp = this.getGlobalRequestProcessor(name);
		if (grp == null) {
			return null;
		}
		MuninGraph graph = new MuninGraph("Tomcat volume of " + tpName, GRAPH_CATEGORY);
		graph.setArgs("--base 1000");
		graph.setTotal("total");
		graph.setVlabel("bytes / ${graph_period}");
		graph.setOrder("sent received");

		String sent = mBeanServer.getAttribute(grp, "bytesSent").toString();
		String received = mBeanServer.getAttribute(grp, "bytesReceived").toString();
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
        {
    		HashMap<String, String> line = new HashMap<String, String>();
    		line.put(MuninGraph.LINE_INFO_NAME, "sent");
    		line.put(MuninGraph.LINE_INFO_LABEL, "sent");
    		line.put(MuninGraph.LINE_INFO_TYPE, "DERIVE");
    		line.put(MuninGraph.LINE_INFO_VALUE, sent);
    		lines.add(line);
        }
        {
    		HashMap<String, String> line = new HashMap<String, String>();
    		line.put(MuninGraph.LINE_INFO_NAME, "received");
    		line.put(MuninGraph.LINE_INFO_LABEL, "received");
    		line.put(MuninGraph.LINE_INFO_TYPE, "DERIVE");
    		line.put(MuninGraph.LINE_INFO_VALUE, received);
    		lines.add(line);
        }


        graph.setLineInfo(lines);
		return graph;
	}


	/**
	 * スレッド数グラフを取得する .
	 * @param opt コネクタ名を指定する.
	 * <pre>
	 * コネクタ名を指定しない場合、8009を含むコネクタを表示する.
	 * </pre>
	 * @return 作成したグラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getAccessGraph(final String opt) throws Exception {
		ObjectName threadpool = getThreadPool(opt);
		if (threadpool == null) {
			return null;
		}

		String name = threadpool.getKeyProperty("name");
		ObjectName grp = this.getGlobalRequestProcessor(name);
		if (grp == null) {
			return null;
		}

		String requestCount = mBeanServer.getAttribute(grp, "requestCount").toString();

		MuninGraph graph = new MuninGraph("Tomcat accesses of " + name, GRAPH_CATEGORY);
		graph.setArgs("--base 1000");
		graph.setVlabel("accesses / ${graph_period}");
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> line = new HashMap<String, String>();
		line.put(MuninGraph.LINE_INFO_NAME, "accesses");
		line.put(MuninGraph.LINE_INFO_LABEL, "Accesses");
		line.put(MuninGraph.LINE_INFO_VALUE, requestCount);
		line.put(MuninGraph.LINE_INFO_TYPE, "DERIVE");
		line.put(MuninGraph.LINE_INFO_MAX, "1000000");
		line.put(MuninGraph.LINE_INFO_MIN, "0");
		lines.add(line);
		graph.setLineInfo(lines);
		return graph;
	}



	/**
	 * スレッド数グラフを取得する .
	 * @param opt コネクタ名を指定する.
	 * <pre>
	 * コネクタ名を指定しない場合、8009を含むコネクタを表示する.
	 * </pre>
	 * @return 作成したグラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getJvmMemoryGraph(final String opt) throws Exception {
		MuninGraph graph = new MuninGraph("Tomcat JVM memory", GRAPH_CATEGORY);
		graph.setArgs("--base 1024 -l 0");
		graph.setVlabel("Bytes");
		graph.setOrder("free used max");
		long free = Runtime.getRuntime().freeMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = total - free;
		long max = Runtime.getRuntime().maxMemory();
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
		{
			HashMap<String, String> line = new HashMap<String, String>();
			line.put(MuninGraph.LINE_INFO_NAME, "free");
			line.put(MuninGraph.LINE_INFO_LABEL, "free bytes");
			line.put(MuninGraph.LINE_INFO_VALUE, Long.toString(free));
			line.put(MuninGraph.LINE_INFO_DRAW, "AREA");
			lines.add(line);

		}
		{
			HashMap<String, String> line = new HashMap<String, String>();
			line.put(MuninGraph.LINE_INFO_NAME, "used");
			line.put(MuninGraph.LINE_INFO_LABEL, "used bytes");
			line.put(MuninGraph.LINE_INFO_VALUE, Long.toString(used));
			line.put(MuninGraph.LINE_INFO_DRAW, "STACK");
			lines.add(line);

		}
		{
			HashMap<String, String> line = new HashMap<String, String>();
			line.put(MuninGraph.LINE_INFO_NAME, "max");
			line.put(MuninGraph.LINE_INFO_LABEL, "maximum bytes");
			line.put(MuninGraph.LINE_INFO_VALUE, Long.toString(max));
			line.put(MuninGraph.LINE_INFO_DRAW, "LINE2");
			lines.add(line);

		}

        graph.setLineInfo(lines);
		return graph;
	}


	/**
	 * Tomcat JVMのGC回数グラフを取得する .
	 * @return GC回数グラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getGCCountGraph() throws Exception {
		MuninGraph graph = new MuninGraph("Tomcat JVM gc count ", GRAPH_CATEGORY);
		graph.setArgs("--base 1000");
		graph.setVlabel("gc count / ${graph_period}");
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();

		List<GarbageCollectorMXBean> mBeans = ManagementFactory.getGarbageCollectorMXBeans();
		int idx = 0;
		for (GarbageCollectorMXBean mb : mBeans) {
	        HashMap<String, String> line = new HashMap<String, String>();
			line.put(MuninGraph.LINE_INFO_NAME, "gc" + (idx++));
			line.put(MuninGraph.LINE_INFO_LABEL, mb.getName());
			line.put(MuninGraph.LINE_INFO_VALUE, Long.toString(mb.getCollectionCount()));
			line.put(MuninGraph.LINE_INFO_TYPE, "DERIVE");
			lines.add(line);
		}
		graph.setLineInfo(lines);
		return graph;
	}

	/**
	 * Tomcat JVMのGC時間グラフを取得する .
	 * @return GC回数グラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getGCTimeGraph() throws Exception {
		MuninGraph graph = new MuninGraph("Tomcat JVM gc time ", GRAPH_CATEGORY);
		graph.setArgs("--base 1000");
		graph.setVlabel("gc time(ms) / ${graph_period}");
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();

		List<GarbageCollectorMXBean> mBeans = ManagementFactory.getGarbageCollectorMXBeans();
		int idx = 0;
		for (GarbageCollectorMXBean mb : mBeans) {
	        HashMap<String, String> line = new HashMap<String, String>();
			line.put(MuninGraph.LINE_INFO_NAME, "gc" + (idx++));
			line.put(MuninGraph.LINE_INFO_LABEL, mb.getName());
			line.put(MuninGraph.LINE_INFO_VALUE, Long.toString(mb.getCollectionTime()));
			line.put(MuninGraph.LINE_INFO_TYPE, "DERIVE");
			lines.add(line);
		}
		graph.setLineInfo(lines);
		return graph;
	}

	/**
	 * Memory Poolの状態グラフを取得する .
	 * @return Memory Poolの状態グラフ.
	 * @throws Exception 例外.
	 */
	private MuninGraph getMemoryPoolGraph() throws Exception {
		MuninGraph graph = new MuninGraph("Tomcat JVM memory pool", GRAPH_CATEGORY);
		graph.setArgs("--base 1024 -l 0");
		graph.setVlabel("Bytes");
        List<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
		List<MemoryPoolMXBean> mpools = ManagementFactory.getMemoryPoolMXBeans();
		int idx = 0;
		for (MemoryPoolMXBean mpool : mpools) {
	        HashMap<String, String> uline = new HashMap<String, String>();
			uline.put(MuninGraph.LINE_INFO_NAME, "pool" + (idx++));
			uline.put(MuninGraph.LINE_INFO_LABEL, mpool.getName() + " Used");
			uline.put(MuninGraph.LINE_INFO_VALUE, Long.toString(mpool.getUsage().getUsed()));
			uline.put(MuninGraph.LINE_INFO_DRAW, (idx == 1 ? "AREA" : "STACK"));
			lines.add(uline);
	        HashMap<String, String> fline = new HashMap<String, String>();
			fline.put(MuninGraph.LINE_INFO_NAME, "pool" + (idx++));
			fline.put(MuninGraph.LINE_INFO_LABEL, mpool.getName() + " Free");
			fline.put(MuninGraph.LINE_INFO_VALUE, Long.toString(mpool.getUsage().getMax() - mpool.getUsage().getUsed()));
			fline.put(MuninGraph.LINE_INFO_DRAW, (idx == 1 ? "AREA" : "STACK"));
			lines.add(fline);
		}
		graph.setLineInfo(lines);
		return graph;
	}

	/**
	 * HTTP GETメソッドの処理 .
	 *
	 * @param req httpリクエスト.
	 * @param resp http応答.
	 * @throws ServletException サーブレット例外.
	 * @throws IOException IO例外.
	 *
	 */
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		PrintWriter out = resp.getWriter();
		try {
			if (this.host != null) {
				log.info("host-name:" + this.host.getName());
				String graph = req.getParameter("g");
				String arg = req.getParameter("a");
				String opt = req.getParameter("o");
				log.info("graph=" + graph + ",arg=" + arg + ", opt=" + opt);
				if ("autoconf".equals(arg)) {
					out.println("yes");
				} else {
					MuninGraph g = null;
					if ("sessions".equals(graph)) {
						g = this.getSessionsGraph(opt);
					} else if ("threads".equals(graph)) {
						g = this.getThreadsGraph(opt);
					} else if ("jvm".equals(graph)) {
						g = this.getJvmMemoryGraph(opt);
					} else if ("access".equals(graph)) {
						g = this.getAccessGraph(opt);
					} else if ("volume".equals(graph)) {
						g = this.getVolumeGraph(opt);
					} else if ("gccount".equals(graph)) {
						g = this.getGCCountGraph();
					} else if ("gctime".equals(graph)) {
						g = this.getGCTimeGraph();
					} else if ("mempool".equals(graph)) {
						g = this.getMemoryPoolGraph();
					}
					if (g != null) {
						if ("config".equals(arg)) {
							out.print(g.getConfigInfo());
						} else {
							out.print(g.getValueInfo());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			out.close();
		}
	}
}
