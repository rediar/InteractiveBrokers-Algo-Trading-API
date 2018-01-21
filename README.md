# InteractiveBrokers-Algo-System
Java/MySQL example of Interactive Brokers API for algorithmic trading. Includes live price feed, handling ib price quote limits, order tracking system, margin tracking system, order handling, option chain information request, and kill switch
- Requires either TWS or IB Gateway. The API scripts initiate a live socket connection to the desktop TWS/IB Gateway application, which routes requests
- Requires US Securities Snapshot and Futures Value Bundle ($10/month), US Equity and Options Add-On Streaming Bundle ($4.95/month)
- I simultaneously run the price feed/other reading processes on IB Gateway paper trading (port 4002) and the trading strategy/other writing processes to TWS (port 7496)
- API function documentation: http://interactivebrokers.github.io/tws-api/

# Set up MySQL database
- set up the following tables
- IBAlgoSystem.price: symbol (VARCHAR(10)), secType (VARCHAR(10)), currency (VARCHAR(3)), expiry (VARCHAR(8)), strike (DOUBLE), callorput (VARCHAR(1)), multiplier (VARCHAR(10)), bid (DOUBLE), ask (DOUBLE), last (DOUBLE), close (DOUBLE), bugCounter (INT), updateTime (BIGINT)
- IBAlgoSystem.MasterChainList: entry (INT), active (VARCHAR(1)), symbol (VARCHAR(10)), secType (VARCHAR(10)), exchange (VARCHAR(20)), currency (VARCHAR(3)), expiry (VARCHAR(8)), strike (DOUBLE), callorput (VARCHAR(1)), multiplier (VARCHAR(10)), pennyPilot (VARCHAR(1)), moneyness (VARCHAR(3))
- IBAlgoSystem.orderTracking: orderID (INT), status (VARCHAR(20)), filled (INT), remaining (INT), avgFillPrice (DOUBLE)
- IBAlgoSystem.counter: counterID (INT)
- IBAlgoSystem.margin: AccruedCash (DOUBLE), AvailableFunds (DOUBLE), BuyingPower (DOUBLE), Cushion (DOUBLE), EquityWithLoanValue (DOUBLE), ExcessLiquidity (DOUBLE), FullAvailableFunds (DOUBLE), FullExcessLiquidity (DOUBLE), FullInitMarginReq (DOUBLE), FullMaintMarginReq (DOUBLE), GrossPositionValue (DOUBLE), InitMarginReq (DOUBLE), LookAheadAvailableFunds (DOUBLE), LookAheadExcessLiquidity (DOUBLE), LookAheadInitMarginReq (DOUBLE), LookAheadMaintMarginReq (DOUBLE), LookAheadNextChange (DOUBLE), MaintMarginReq (DOUBLE), NetLiquidation (DOUBLE), TotalCashValue (DOUBLE)

# SimplePriceFeed
- Read in TickerList.csv - a list of contracts to receive live price streams from
- TickerList.csv fields: Active (T/F) | symbol | secType | exchange | currency | expiry | strike | right | multiplier
- Output prices to IBAlgoSystem.price

# LargePriceFeed
- Read in IBAlgoSystem.MasterChainList - a list of options contracts to receive live price streams from
- The number of entries may exceed ib's feed limit (FEED_LIMIT)
- This script divides the entries into smaller batches to submit to ib
- Output prices to IBAlgoSystem.price

# FetchOptionsChain
- Read in TickerList.csv, a list of contracts to generate options chains from
- TickerList.csv fields: Active (T/F) | symbol | secType | exchange | currency | expiry | strike | right | multiplier
- Read in PennyPilot.csv, a list of Penny Pilot tickers
- Output option chain to IBAlgoSystem.MasterChainList

# TradeStrategy
- template to implement trade strategies

# KillSwitch
- kills all active orders
