<%@ page language="java" contentType="text/html; charset=EUC-KR"
    pageEncoding="EUC-KR"%>
<!DOCTYPE html>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>로그인 창</title>
</head>
<body>
	<h1>Security Login</h1>
	<form action="/auth/loginProcess" method="POST">
		<input type="email" name="email" placeholder="Email" /><br /> <input
			type="password" name="password" placeholder="Password" /><br /> <input
			type="submit" value="로그인">
	</form>
	<br />

	<h1>Social Login</h1>
	<br />
	<!-- javascript:; 는 클릭해도 반응을 없게 하는 키워드 -->
	<a href="javascript:;" class="btn_social" data-social="google"> <img
		src="https://pngimage.net/wp-content/uploads/2018/06/google-login-button-png-1.png"
		alt="google" width="357px" height="117px">
	</a>
	<br />
	<a href="javascript:;" class="btn_social" data-social="facebook"> <img
		src="https://pngimage.net/wp-content/uploads/2018/06/login-with-facebook-button-png-transparent-1.png"
		alt="facebook" width="357px" height="117px">
	</a>

	<script>
		var social = document.getElementsByClassName("btn_social");
		for (var i = 0; i < social.length; ++i) {
			social[i].addEventListener("click", function() {
				var socialType = this.getAttribute("data-social");
				location.href = "/oauth2/authorization/" + socialType;
				console.log("click!!!");
			}, false);
		}
	</script>

</body>
</html>