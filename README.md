# InteractiveBrokers Algo Trading API
This Java/MySQL framework implements the Interactive Brokers API for algorithmic trading. Included are all essential components to support a basic trading execution system: live price feed, handling for IB price quote limits, order tracking system, margin tracking system, handling for order submission and execution, option chain information request, and kill switches. A system based on this framework, with additional custom modules and proprietary strategies is currently being used to profitably trade US equities and equity options
- Requires either TWS or IB Gateway (desktop applications provided by IB) and IB Java API 9.71 (API library package). The API scripts initiate a live socket connection to the desktop TWS and IB Gateway application, which routes requests
- Requires US Securities Snapshot and Futures Value Bundle ($10/month), US Equity and Options Add-On Streaming Bundle ($4.95/month) for live price feed. If solely using the framework for executing orders, then these add on costs are not necessary; it is possible to use another data source for live price feeds such as IEX, which is free
- It's recommended to simultaneously run price feed and solely reading processes on an IB Gateway paper trading instance (port 4002 in the example) and trade execution/other writing processes on a TWS instance (port 7496 in the example)
- API function documentation: http://interactivebrokers.github.io/tws-api/
![Flowchart](https://github.com/rediar/InteractiveBrokers-Algo-System/blob/master/IB%20Algo%20Flowchart.png)

# Setup instructions
### Set up MySQL database ###
Set up the following tables:

Price: 
- IBAlgoSystem.price: symbol (VARCHAR(10)), secType (VARCHAR(10)), currency (VARCHAR(3)), expiry (VARCHAR(8)), strike (DOUBLE), callorput (VARCHAR(1)), multiplier (VARCHAR(10)), bid (DOUBLE), ask (DOUBLE), last (DOUBLE), close (DOUBLE), bugCounter (INT), updateTime (BIGINT)

Option details:
- IBAlgoSystem.MasterChainList: entry (INT), active (VARCHAR(1)), symbol (VARCHAR(10)), secType (VARCHAR(10)), exchange (VARCHAR(20)), currency (VARCHAR(3)), expiry (VARCHAR(8)), strike (DOUBLE), callorput (VARCHAR(1)), multiplier (VARCHAR(10)), pennyPilot (VARCHAR(1)), moneyness (VARCHAR(3))

Orders:
- IBAlgoSystem.orderTracking: orderID (INT), status (VARCHAR(20)), filled (INT), remaining (INT), avgFillPrice (DOUBLE)

Execution failure retry counter:
- IBAlgoSystem.counter: counterID (INT)

Cash, margin and risk balance:
- IBAlgoSystem.margin: AccruedCash (DOUBLE), AvailableFunds (DOUBLE), BuyingPower (DOUBLE), Cushion (DOUBLE), EquityWithLoanValue (DOUBLE), ExcessLiquidity (DOUBLE), FullAvailableFunds (DOUBLE), FullExcessLiquidity (DOUBLE), FullInitMarginReq (DOUBLE), FullMaintMarginReq (DOUBLE), GrossPositionValue (DOUBLE), InitMarginReq (DOUBLE), LookAheadAvailableFunds (DOUBLE), LookAheadExcessLiquidity (DOUBLE), LookAheadInitMarginReq (DOUBLE), LookAheadMaintMarginReq (DOUBLE), LookAheadNextChange (DOUBLE), MaintMarginReq (DOUBLE), NetLiquidation (DOUBLE), TotalCashValue (DOUBLE)

# Module descriptions

### SimplePriceFeed ###
Read in TickerList.csv - a presaved list of contracts to receive live price feeds from. Here, a contract is defined as either a specific option, or stock
- TickerList.csv fields: Active (T/F) | symbol | secType | exchange | currency | expiry | strike | right | multiplier
- Output prices to the IBAlgoSystem.price MySQL table

### LargePriceFeed ###
Read in IBAlgoSystem.MasterChainList - a list of options contracts to receive live price feeds from
- The number of entries may exceed IB's feed limit (FEED_LIMIT). In that case, this script divides the entries into smaller batches to submit to IB
- Output prices to IBAlgoSystem.price MySQL table

### FetchOptionsChain ###
Read in TickerList.csv, a presaved list of contracts to receive live price feeds from. Here, a contract is defined as either a specific option, or stock
- TickerList.csv fields: Active (T/F) | symbol | secType | exchange | currency | expiry | strike | right | multiplier
Read in PennyPilot.csv, a presaved list of Penny Pilot tickers
- Output option chain to IBAlgoSystem.MasterChainList MySQL table

### TradeStrategy ###
Template to implement trade strategies - call price feed, submit order execution requests, manage orders and risk
- Output order management to IBAlgoSystem.orderTracking MySQL table
- Output margin management to IBAlgoSystem.margin MySQL table

### KillSwitch ###
Kills all active orders

# Simple walkthrough description

### Receive price feed ###
1. Start an IB Gateway instance on port 4002 (or custom defined port number)
2. Define the contracts to receive stock and/or option price feeds for in TickerList.csv
3. Execute SimplePriceFeed to start price feeds for contracts in TickerList.csv
4. If TickerList.csv contains only stock tickers, execute FetchOptionsChain to generate the option chain for those tickers
5. Execute LargePriceFeed to start price feeds for option chains
6. Restart SimplePriceFeed or LargePriceFeed if problems occur

### Execute trade strategy ###
1. Start a TWS instance on port 7496 (or custom defined port number)
2. Execute TradeStrategy

### Trigger KillSwitch ###
In event of emergency only:
1. Terminate TradeStrategy
2. Execute KillSwitch to remove outstanding orders

Note the KillSwith does not impact SimplePriceFeed or LargePriceFeed (price feed)
