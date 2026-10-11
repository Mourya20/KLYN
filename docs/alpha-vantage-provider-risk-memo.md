# Alpha Vantage Provider Risk Memo

## Account quota

The actual quota for the owner's Alpha Vantage key was not verified in this workspace. The current configuration allows at most five requests per run with a twelve-second delay, which is only an operational default and must not be treated as proof of the key's quota. Confirm the account's current per-minute and per-day limits in the provider account before changing those values.

## Indian listing symbols

The repository contains configured examples such as `RELIANCE.BSE`, but no provider-account or provider-documentation check was performed here to establish the supported symbol format for Indian listings. Treat every NSE/BSE mapping as `UNVERIFIED` until a reviewed response confirms it. A provider response for a requested symbol is recorded as `RESPONDING`; it does not verify exchange or currency.

## Alternative bulk source

The `MarketDataProvider` abstraction can support a bulk end-of-day source behind another implementation, but provider licensing, redistribution rights, rate limits, symbol coverage, and terms of use must be reviewed before selecting one. No provider change is proposed in Stage 3.