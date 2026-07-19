# LimonAntiCheat config

fly:
  enabled: true
  # сколько тиков подряд игрок может находиться в воздухе без падения, прежде чем это подозрительно
  max-air-time-ticks: 40
  # штраф за нарушение
  violation-weight: 1

speed:
  enabled: true
  # максимальная горизонтальная скорость блоков/тик при спринте (обычный спринт ~0.28-0.3)
  max-sprint-speed: 0.42
  # максимальная скорость при ходьбе
  max-walk-speed: 0.32
  violation-weight: 1

combat:
  enabled: true
  # максимальная дистанция удара (ванильный reach ~3.0-3.5, даём небольшой запас на лаг)
  max-reach: 4.2
  # максимальный угол (в градусах) между направлением взгляда и целью - killaura часто бьёт "не глядя"
  max-angle-degrees: 60
  # окно в миллисекундах, за которое нельзя бить больше одной РАЗНОЙ цели (мульти-аура)
  multi-target-window-ms: 150
  # проверять что между игроком и целью нет твёрдого блока (атака сквозь стену)
  check-line-of-sight: true
  violation-weight: 2

noslow:
  enabled: true
  # макс. скорость при блокировании щитом/еде/натягивании лука (ваниль замедляет игрока)
  max-speed-while-using-item: 0.22
  violation-weight: 1

autoclicker:
  enabled: true
  # человеческий предел CPS (кликов в секунду) на длинной дистанции - выше considered читом
  max-legit-cps: 14
  # минимальный разброс интервалов между кликами (мс) - ниже = подозрительно ровный ритм (макрос)
  min-stddev-ms: 15
  violation-weight: 2

scaffold:
  enabled: true
  # минимальный интервал между установкой блоков (мс) при взгляде вниз
  min-place-interval-ms: 110
  # угол наклона камеры вниз (градусы), после которого считаем что игрок строит мост под собой
  min-pitch-looking-down: 70
  violation-weight: 1

nofall:
  enabled: true
  # минимальная высота падения (блоков), после которой урон ОБЯЗАН быть
  min-fall-distance: 4.0
  violation-weight: 2

jesus:
  enabled: true
  # минимальное горизонтальное перемещение за тик, чтобы засчитать его в счётчик "стоит на воде"
  min-horizontal-move: 0.02
  # сколько тиков ПОДРЯД нужно "стоять" на воде, прежде чем флагать (защита от ложных срабатываний на границе воды)
  min-consecutive-ticks: 12
  violation-weight: 2

criticals:
  enabled: true
  # во сколько раз фактический базовый урон должен превышать "чистый" атрибут атаки, чтобы считать его критом
  crit-factor-threshold: 1.35
  violation-weight: 3

fastbreak:
  enabled: true
  # насколько быстрее ожидаемого времени можно ломать блок, прежде чем это подозрительно (0.5 = вдвое быстрее)
  leniency-multiplier: 0.5
  violation-weight: 2

blink:
  enabled: true
  # максимальное горизонтальное перемещение (блоков) за один PlayerMoveEvent
  max-horizontal-distance: 8.0
  violation-weight: 4

timer:
  enabled: true
  # интервал проверки в тиках (20 = раз в секунду)
  check-interval-ticks: 20
  # максимум move-событий за один интервал (ваниль ~20/сек, даём запас на лаги/пакет-группировку)
  max-moves-per-window: 24
  violation-weight: 3

# Совместимость со старыми версиями клиента (через ViaVersion, если установлен).
# Игроки на старых версиях (1.16.5-1.20) двигаются/атакуют чуть иначе физически,
# чем актуальная версия сервера - даём им запас, чтобы не ловить ложные срабатывания.
version-compat:
  enabled: true
  # protocol-версия, ниже которой клиент считается "старым" (767 = 1.21)
  legacy-protocol-threshold: 767
  # во сколько раз смягчаем пороги speed/noslow/combat для старых клиентов
  leniency-multiplier: 1.3

# что делать при накоплении нарушений
punishment:
  # уровень, при котором стафф получает РАННЕЕ уведомление "подозрение в читах"
  # (до алерта - чтобы успеть зайти /spec и проверить лично)
  suspicion-threshold: 4
  # сколько нарушений (violation level) до полноценного алерта персоналу
  alert-threshold: 8
  # сколько нарушений до "серьёзного" алерта (мигающий, привлекающий внимание)
  serious-threshold: 20
  # ВКЛЮЧАТЬ ЛИ автоматический кик игрока - по умолчанию ВЫКЛЮЧЕНО.
  # Античит только следит и репортит стаффу, финальное решение - за живым человеком через /spec.
  # Это и есть подход топовых серверов: софт не наказывает сам, только помогает найти нарушителя.
  auto-kick-enabled: false
  kick-threshold: 30
  # автобан - по умолчанию тоже выключен, банить руками после личной проверки через /spec
  auto-ban-enabled: false
  ban-threshold: 50
  # время в секундах, через которое уровень нарушений постепенно спадает (anti-false-positive)
  decay-interval-seconds: 45
  decay-amount: 2

alerts:
  # право, которое должно быть у стаффа чтобы видеть алерты
  permission: anticheat.admin
  suspicion-message: "&e⚠ &f%player% &7ведёт себя подозрительно &7(&e%check%&7). Проверь: &f/spec %player%"
  message: "&c[AC] &f%player% &7нарушение: &e%check% &7(уровень: &c%level%&7) &7- &f/spec %player%"
  serious-message: "&4&l⛔ СЕРЬЁЗНОЕ ПОДОЗРЕНИЕ &c%player% &7(&e%check%&7, уровень &c%level%&7) &f/spec %player% §cСРОЧНО"

# Контакт для апелляций при бане (показывается на экране кика)
ban-appeal-contact: "Telegram: @Milan4ck3456"

log-to-console: true
