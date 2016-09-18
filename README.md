## Запуск системы

В качестве системы управления проектом используется sbt.

Система понимает следущие параметры командной строки:

- `-c, --clients <value>  path to clients.txt`
- `-o, --orders <value>   path to orders.txt`
- `-r, --result <value>   path to result.txt`

По умолчанию список клиентов берется из файла `sample/clients.txt`,
список заказов - из файла `sample/orders.txt`, результат складывается
в файл `result.txt`

Для удобства запуска системы создан скрипт `bin/trade-matching`.
