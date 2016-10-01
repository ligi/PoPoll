This project was created in the context of the /post/bank hackathon. It queries the postbank API if there is a certain transaction in the transaction list of the connected account. This was done for the following use-case - but is not limited to it:

Currently when trading selling bitcoins on [BitSquare](https://github.com/bitsquare/bitsquare) and expecting a SEPA transaction you have to manually check if the transaction happened. With the help of the postbank API we can automate this process.

But instead of directly adding the postbank polling to the ( already big ) bitsquare codebase this small project was created. This way the code in the bitsquare client can stay the same for different bank-APIs.

Also it reduces the attack surface. The part that holds the account credentials for the postbank account can stay very small and be audited very fast.

It will start a server on 4244 which you can query like this:

```
⋊> ~ curl "http://localhost:4244/check?amount=350.00&fromname=Mario%20Liebeaeqz&reference=FA1233C55" 
{"result":"found"}⏎
```

```
⋊> ~ curl "http://localhost:4244/check?amount=350.00&fromname=Mario%20Liebeaeqz&reference=ABC123" 
{"result":"notfound"}⏎
```


```
⋊> ~ curl "http://localhost:4244/check?fromname=Mario%20Liebeaeqz&reference=miete"
{"error":"you need to pass one amount"}⏎
```
