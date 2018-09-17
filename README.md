Нужно написать сервис который конвертирует валюты.
Сервис можно выбрать любой (https://www.google.ru/search?newwindow=1&hl=ru&ei=JSqxWr_5DsuksgG8qpHIDQ&q=open+currency+exchange+api&oq=open+currency+exchange+api&gs_l=psy-ab.3..0i203k1.11984.16616.0.17202.23.19.1.3.3.0.174.1530.14j4.18.0....0...1c.1.64.psy-ab..1.22.1538...0j0i10k1j0i10i203k1j0i22i30k1j0i22i10i30k1.0.PTzk5maezaY), главное чтобы можно было протестировать работу вашего приложения.
Требования:
	Обязательные:
		Использовать последние стабильные версии Scala, sbt и akka-http
		Никаких блокировок потоков!
		Присылать ссылку на git репку на github или bitbucket
		Логировать запрос и ответ в файл, а так же время выполнения запроса (прим: log4j2)
		Обязательно написать интеграционные тесты. Именно они и будут запускаться для проверки задания. (Фреймворк тестирования любоей из скаловских)
	Желательно:
		Использовать последнюю стабильную версию akka-streams, akka-actors
	Приветствуется:
		Использовать последнюю стабильную версию cats, monix. Если придумаете куда их вставить, как заюзать. В любом случает приветствтвуется попытка показать как вы умеете пользоваться каким-либо крутым, удобным инструментом/либой/шаблоном/плагином и т.п.
Запрос: Http POST
{
  "data": [
    {
      "currencyFrom" : "RUB",
      "currencyTo" : "USD",
      "valueFrom" : 15.65
    },
    {
      "currencyFrom" : "RUB",
      "currencyTo" : "EUR",
      "valueFrom" : 20.0
    }
  ]
}
Ответ:
{
  "data": [
    {
      "currencyFrom" : "RUB",
      "currencyTo" : "USD",
      "valueFrom" : 15.65,
      "valueTo" : 45.47
    },
    {
      "currencyFrom" : "RUB",
      "currencyTo" : "EUR",
      "valueFrom" : 20.0,
      "valueTo" : 80.0
    }
  ],
  "errorCode": 0,
  "errorMessage": "No errors"
}