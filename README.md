# Yomishio

Yomishio is an independent fork of [TachiyomiAZ](https://github.com/az4521/TachiyomiAZ). The original application, architecture, and upstream development are the work of the TachiyomiAZ contributors, who retain full credit for them.

This fork exists because a separate build was necessary to deliver fixes and device-specific improvements that were not available in the original app: safe TachiyomiAZ data migration, reliable tracker restoration and automatic statuses, Bigme color-inversion compensation, lower storage pressure, and Yomishio branding. The name combines the Japanese ideas of reading (`yomi`) and tide (`shio`).

## Highlights

- Automatic tracker workflow: newly linked titles start as Plan to Read/Planning/Want to Read, using each service's native label.
- Starting a title automatically changes its tracker state to Reading/Current using the native status of every supported service, while preserving explicit rereading states.
- MyAnimeList, AniList, Kitsu, Shikimori, and Bangumi support.
- Imports the original TachiyomiAZ legacy and full backup formats without converting or discarding tracking data.
- One universal APK supports ARMv7, ARM64, x86, and x86_64.
- Release builds use code and resource shrinking plus zip alignment for slower devices.

## Compatibility

Yomishio deliberately keeps the original internal source IDs, database models, tracker IDs, deep-link schemes, and backup serializers. This allows backups created by TachiyomiAZ to be restored normally while the separate application ID avoids overwriting an existing installation.

## Legal

Yomishio is distributed under the Apache License 2.0 and retains the copyright, license, commit history, and attribution of TachiyomiAZ and its own upstream projects. Yomishio is not presented as the original TachiyomiAZ project or as an official release from its maintainers. Use it only with content you are authorized to access.
