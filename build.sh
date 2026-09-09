#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

rounds="${1:-3}"
if [[ ! "$rounds" =~ ^[1-9][0-9]*$ ]]; then
    printf 'Использование: ./build.sh [положительное число запусков]\n' >&2
    exit 2
fi

if ! command -v javac >/dev/null || ! command -v java >/dev/null; then
    printf 'Нужен JDK 17 или новее: команды javac и java.\n' >&2
    exit 1
fi

rm -rf -- out
mkdir -p out/main out/test

# Ответные файлы javac позволяют обойтись без Maven/Gradle и зависимостей.
find src/main/java -type f -name '*.java' | sort > out/main-sources.txt
find src/test/java -type f -name '*.java' | sort > out/test-sources.txt

javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
    -d out/main @out/main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
    -cp out/main -d out/test @out/test-sources.txt

for ((round = 1; round <= rounds; round++)); do
    printf '\nЗапуск %d/%d в новой JVM\n' "$round" "$rounds"
    java -cp out/main:out/test examples.singletons.SingletonConcurrencyTest
done
