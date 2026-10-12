# YMOBD XU7 diagnostic notes

## Changes in this revision
- Real ELM327 initialization now selects ISO 14230-4 KWP Fast Init (`ATSP5`) instead of `ATSP0`.
- Headers are visible (`ATH1`) during diagnosis so the ECU response address can be inspected.
- Multi-line commands are executed one line at a time instead of being sent as one Bluetooth payload.
- ECU configuration UI changes state only after a non-negative ECU response.
- Configuration requests attempt a standard KWP diagnostic session (`10 81`) before the proprietary routine.
- The old CAN IDs `7A0` and `7E0` were removed from the KWP routines; they were being mixed with KWP transport.
- The fake actuator command `30 01 01` was removed.

## Important
The exact proprietary routines for Siemens/Crouse XU7 body/engine variants cannot safely be inferred from the screenshots alone. The terminal should first be used on the real car to capture `ATDP`, `0100`, and the response headers. Once those bytes are known for the user's ECU, the exact physical addressing and Routine/Data Identifiers can be added.
