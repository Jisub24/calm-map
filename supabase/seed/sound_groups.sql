-- =====================================================
-- sound_groups — YAMNet 라벨 → 소리 종류 7종 번역표
-- 실행: Supabase 대시보드 → SQL Editor → 붙여넣고 Run
-- 먼저 필요: migrations/020_user_profiles_levels.sql (소리 종류 7종 확정)
-- =====================================================
--
-- 10/18 홍대 답사 녹음 7건을 YAMNet 으로 돌려 실제로 나온 라벨 위주로 넣었다
-- (analysis/sound_analysis.py). 경보 · 울음 · 공사는 답사에서 안 나왔지만
-- 답변서 묶음과 앱 민감 요인 선택지에 있어서 넣는다.
--
-- 앱 android/.../data/sound/SoundGroups.kt 에 같은 표가 있어야 한다. 앱은 측정 중에
-- 알림음 · 돌발을 실시간으로 세야 해서 표를 안에 들고 있고, batch 는 저장된
-- 원본 라벨을 소리 탭 · 우리 아이 맞춤 계산에 쓰려고 이 표를 읽는다.
-- 둘이 다르면 앱이 센 횟수와 batch 가 묶은 비율이 어긋난다. 한쪽을 고치면 다른 쪽도 고친다.
--
-- 돌발 횟수 = alert + impact + alarm + loud_voice. 한 라벨은 한 종류에만 넣는다.
-- weight 는 전부 1. 한 라벨을 두 종류에 나눠 담는 경우가 아직 없다.
--
-- 다시 실행해도 안전하다 (같은 라벨 · 종류는 건너뜀).

insert into sound_groups (yamnet_label, sound_type, weight) values
  -- alert 알림음. 안내방송 앞 "띠링". 투썸 알림음 5개가 이 라벨들로 잡혔다
  ('Chime',                        'alert', 1),
  ('Wind chime',                   'alert', 1),
  ('Bell',                         'alert', 1),
  ('Jingle bell',                  'alert', 1),
  ('Tubular bells',                'alert', 1),
  ('Mallet percussion',            'alert', 1),
  ('Marimba, xylophone',           'alert', 1),
  ('Glockenspiel',                 'alert', 1),
  ('Vibraphone',                   'alert', 1),
  ('Ding',                         'alert', 1),
  ('Ding-dong',                    'alert', 1),
  ('Doorbell',                     'alert', 1),
  ('Steelpan',                     'alert', 1),

  -- impact 부딪힘. 투썸 그릇 · 컵 소리를 직접 들어 확인
  ('Dishes, pots, and pans',       'impact', 1),
  ('Cutlery, silverware',          'impact', 1),
  ('Chink, clink',                 'impact', 1),
  ('Glass',                        'impact', 1),
  ('Clang',                        'impact', 1),
  ('Bang',                         'impact', 1),
  ('Slam',                         'impact', 1),
  ('Smash, crash',                 'impact', 1),
  ('Shatter',                      'impact', 1),
  ('Breaking',                     'impact', 1),
  ('Crack',                        'impact', 1),
  ('Clatter',                      'impact', 1),

  -- alarm 경보 · 사이렌. 답변서 묶음 1 · 앱 자극 "경보음·사이렌"
  ('Alarm',                        'alarm', 1),
  ('Siren',                        'alarm', 1),
  ('Civil defense siren',          'alarm', 1),
  ('Beep, bleep',                  'alarm', 1),
  ('Buzzer',                       'alarm', 1),
  ('Smoke detector, smoke alarm',  'alarm', 1),
  ('Fire alarm',                   'alarm', 1),

  -- loud_voice 사람의 큰 소리. 답변서 묶음 2 · 앱 자극 "사람의 큰 소리 (고함 · 울음)"
  ('Screaming',                    'loud_voice', 1),
  ('Yell',                         'loud_voice', 1),
  ('Shout',                        'loud_voice', 1),
  ('Children shouting',            'loud_voice', 1),
  ('Crying, sobbing',              'loud_voice', 1),
  ('Baby cry, infant cry',         'loud_voice', 1),
  ('Whimper',                      'loud_voice', 1),

  -- speech 말소리. 측정 7건 모두 1위 라벨이 Speech 였다
  ('Speech',                       'speech', 1),
  ('Child speech, kid speaking',   'speech', 1),
  ('Conversation',                 'speech', 1),
  ('Narration, monologue',         'speech', 1),
  ('Hubbub, speech noise, speech babble', 'speech', 1),
  ('Crowd',                        'speech', 1),
  ('Chatter',                      'speech', 1),
  ('Laughter',                     'speech', 1),

  -- music 음악. 카페 · 마트 배경음악
  ('Music',                        'music', 1),
  ('Pop music',                    'music', 1),
  ('Background music',             'music', 1),
  ('Singing',                      'music', 1),
  ('Electronic music',             'music', 1),
  ('Jingle (music)',               'music', 1),
  ('Musical instrument',           'music', 1),

  -- machine 기계음. 가게 기계(냉장고 · 에어컨 · 계산대) + 공사 (민감 요인 "공사 소음")
  ('Mechanical fan',               'machine', 1),
  ('Air conditioning',             'machine', 1),
  ('Engine',                       'machine', 1),
  ('Vacuum cleaner',               'machine', 1),
  ('Printer',                      'machine', 1),
  ('Cash register',                'machine', 1),
  ('Mechanisms',                   'machine', 1),
  ('Hum',                          'machine', 1),
  ('Buzz',                         'machine', 1),
  ('Whir',                         'machine', 1),
  ('Drill',                        'machine', 1),
  ('Hammer',                       'machine', 1),
  ('Jackhammer',                   'machine', 1),
  ('Power tool',                   'machine', 1),
  ('Sawing',                       'machine', 1)

on conflict (yamnet_label, sound_type) do nothing;

-- 넣지 않는 라벨 (세지 않는다)
--   폰 만짐: Wind noise (microphone) · Heart sounds, heartbeat · Thump, thud · Rustle · Scratch
--            측정하는 사람 쪽 소리. 앱이 "폰을 만지지 마세요" 안내에만 쓴다
--   Speech synthesizer · Echo · Reverberation · Television · Radio
--            예전 안내방송 묶음. 녹음 7건에서 한 번도 안 잡혀서 뺐다
