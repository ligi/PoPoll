package popoll.model.postbank.transaction

data class Content(var amount: String, var currency: String, var purpose: Array<String>, var reference: Reference)