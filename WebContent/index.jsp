<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
	String title = "munin.war[Munin用Tomcat plugin servlet]";
	String version = "1.01";
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title><%=title%> version <%=version %></title>
</head>
<h1><%=title %> version <%=version %></h1>
<h2>概要</h2>
<body>
Munin用のTomcat pluginです。
すでにperl等で作られたものはありますが、似たような機能をjava servletで実装しました。<br/>
muninのpluginの機能は全てservletで実装してあります。<br/>
あとはshell scriptからwget -q -O-を呼ぶだけです。<br/>
このpluginは<a href="http://woontai.dip.jp/munin/">ここ</a>で動いています。<br/>
JDK1.8で動作しているtomcat8で動作を確認しています。

<h2>Install</h2>
1.munin.warを<a href="http://woontai.dip.jp/muninwardownload/">ここ</a>からダウンロードする。<br/>
2.munin.warをtomcatのwebappsにコピーする。<br/>
3.http://localhost:8080/munin/をアクセスして、解説pageが表示されることを確認する。<br/>
4.webapps/munin/shにあるcatalina_*をmuninのplugin directory(centos の場合/usr/share/munin/plugins)にコピーする。<br/>
5.コピーしたcatalina_*に実行権限を与える。(chmod +x catalina_*)<br/>
6.コピーしたpluginを所定のdirectory(centosの場合/etc/munin/plugins)にsymbolic linkするか、以下のコマンドで自動追加する。<br/>
<pre>
/usr/sbin/munin-node-configure --shell | grep catalina | sh
</pre>
7.munin-nodeを再起動(centosの場合はservice munin-node restart)して完了。<br/>

<h2>仕様</h2>
munin.warは以下のようなservletを提供します。<br/>
<pre>

http://localhost:8080/munin/plugin?g=graph&a=arg1&o=option

gにはgraphの種類を指定します。
	sessions	各ApplicationのSession数.
	threads TomcatのThread数.
	access Tomcatのaccess状況.
	volume Tomcatのdata転送量.
	jvm java vmのheapの状況.
	gccount Java VM のgc回数.
	gctime Java VM のgc時間(ms).
	mempool Java VM の各種Memory poolの状態.

aには以下の値を指定します。
	autoconf	常にyesを返す.
	config		Graphに応じたconfig情報を返す.
	上記以外	各値を返す.

oには各機能ごとのoption値を設定します。
	sessionsの場合"o=appname1+appneme2"のようにgraphに表示するapplication名を指定します。
	指定しない場合全てのapplicationになります。
	threads,access,volum の場合"o=http-8080"のようにconnectorの名称を指定します。
	指定しない場合8009を含むコネクター(tomcat6の場合"jk-8009")を選択します。


cataline_*はテスト用に作ったものですが一応autoconfに対応しています。
またmunin-nodeの設定ファイルにURLを指定することもできます。

[catalina*]
env.url http://localhost:8080/munin/plugin


munin.warの中にはjavaのソースファイルも入っています。
eclipseのwarインポート機能を使えば簡単にコンパイル環境ができます。


</pre>
<h2>更新履歴</h2>
1.01 tomcat8.0対応。


<h2>著作権</h2>
MUNINにならいGNU General Public Licenseで公開します。<br/>
このソフトウエアは無料で利用できますが、無保証です。<br/>


</body>
</html>