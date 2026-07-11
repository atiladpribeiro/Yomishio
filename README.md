# Yomishio

Yomishio is a lightweight Android manga reader derived from TachiyomiAZ. The name combines the Japanese ideas of reading (`yomi`) and tide (`shio`).

## Highlights

- Automatic tracker workflow: newly linked titles start as Plan to Read/Planning/Want to Read, using each service's native label.
- Starting a planned title automatically changes its tracker state to Reading/Current.
- MyAnimeList, AniList, Kitsu, Shikimori, and Bangumi support.
- Imports the original TachiyomiAZ legacy and full backup formats without converting or discarding tracking data.
- One universal APK supports ARMv7, ARM64, x86, and x86_64.
- Release builds use code and resource shrinking plus zip alignment for slower devices.

## Compatibility

Yomishio deliberately keeps the original internal source IDs, database models, tracker IDs, deep-link schemes, and backup serializers. This allows backups created by TachiyomiAZ to be restored normally while the separate application ID avoids overwriting an existing installation.

## Legal

Yomishio is distributed under the Apache License 2.0 and retains the copyright and attribution history of the upstream project. Use it only with content you are authorized to access.
