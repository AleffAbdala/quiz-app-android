package com.example.myapplicationquiz

data class QuizModel(
    val id : String,
    val title : String,
    val subtitle : String,
    val time : String,
    val perguntas : List<PerguntaModel>
){
    constructor() : this("","","","",emptyList())
}
data class PerguntaModel(
    val pergunta : String,
    val options : List<String>,
    val correctAnswer : String,
){
    constructor() : this("",emptyList(),"")
}

