object Lab4 extends jsy.util.JsyApplication {
  import jsy.lab4.ast._
  import jsy.lab4.Parser
  
  // To search for things to ask Alok: // Learn exactly whats going on here


  /*
   * CSCI 3155: Lab 4
   * Max Harris
   * 
   * Partner: Kevin Vo
   * Collaborators: Jessica Lynch, Dan Matthews
   */

  /*
   * Fill in the appropriate portions above by replacing things delimited
   * by '<'... '>'.
   * 
   * Replace 'YourIdentiKey' in the object name above with your IdentiKey.
   * 
   * Replace the 'throw new UnsupportedOperationException' expression with
   * your code in each function.
   * 
   * Do not make other modifications to this template, such as
   * - adding "extends App" or "extends Application" to your Lab object,
   * - adding a "main" method, and
   * - leaving any failing asserts.
   * 
   * Your lab will not be graded if it does not compile.
   * 
   * This template compiles without error. Before you submit comment out any
   * code that does not compile or causes a failing assert.  Simply put in a
   * 'throws new UnsupportedOperationException' as needed to get something
   * that compiles without error.
   */
  
  /* Collections and Higher-Order Functions */
  
  /* Lists */
  
  def compressRec[A](l: List[A]): List[A] = l match {
    case Nil | _ :: Nil => l;
    case h1 :: (t1 @ (h2 :: _)) => {
      if (h1 == h2) compressRec(t1)
      else h1 :: compressRec(t1)
    }
  }
  
  def compressFold[A](l: List[A]): List[A] = l.foldRight(Nil: List[A]){ // accum starts as Nil and h starts as last element of list
    (h, acc) => acc match {
      case Nil => {
        if (h == Nil) acc // same as List[A](Nil)
        else h :: acc
      }
      case h1 :: _ => {
        if (h == h1) acc
        else h :: acc
      }
    }
  }
  
  def mapFirst[A](f: A => Option[A])(l: List[A]): List[A] = l match {
    case Nil => l
    case h :: t => f(h) match {
      case None => h :: mapFirst(f)(t)
      case Some(a) => a :: t
    }
  }
  
  /* Search Trees */
  
  sealed abstract class Tree {
    def insert(n: Int): Tree = this match {
      case Empty => Node(Empty, n, Empty)
      case Node(l, d, r) => if (n < d) Node(l insert n, d, r) else Node(l, d, r insert n)
    } 
    
    def foldLeft[A](z: A)(f: (A, Int) => A): A = {
      def loop(acc: A, t: Tree): A = t match {
        case Empty => acc
        case Node(l, d, r) => loop(f( loop(acc, l), d), r)
      }
      loop(z, this)
    }
    
    def pretty: String = {
      def p(acc: String, t: Tree, indent: Int): String = t match {
        case Empty => acc
        case Node(l, d, r) =>
          val spacer = " " * indent
          p("%s%d%n".format(spacer, d) + p(acc, l, indent + 2), r, indent + 2)
      } 
      p("", this, 0)
    }
  }
  case object Empty extends Tree
  case class Node(l: Tree, d: Int, r: Tree) extends Tree
  
  def treeFromList(l: List[Int]): Tree =
    l.foldLeft(Empty: Tree){ (acc, i) => acc insert i }
  
  def sum(t: Tree): Int = t.foldLeft(0){ (acc, d) => acc + d }
  


  // ******************************************************************************************** //
  // ********              !!!!!!!!!!!!  NEED ALOK TO EXPLAIN  !!!!!!!!!!!!!          *********** //
  // ******************************************************************************************** //

  def strictlyOrdered(t: Tree): Boolean = {
    val (b, _) = t.foldLeft((true, None: Option[Int])){
      case ( (acc, None), ele )       => ((acc && true), Some(ele): Option[Int])
      case ( (acc, Some(ele1)), ele)  => if (ele1 < ele) ((acc && true), None) else ((acc && false), None)
    }
    b
  }
  
  /* Type Inference */
  
  // A helper function to check whether a jsy type has a function type in it.
  // While this is completely given, this function is worth studying to see
  // how library functions are used.
  def hasFunctionTyp(t: Typ): Boolean = t match {
    case TFunction(_, _) => true
    case TObj(fields) if (fields exists { case (_, t) => hasFunctionTyp(t) }) => true
    case _ => false
  }
  
  def typeInfer(env: Map[String,Typ], e: Expr): Typ = {
    // Some shortcuts for convenience
    def typ(e1: Expr) = typeInfer(env, e1)
    def err[T](tgot: Typ, e1: Expr): T = throw StaticTypeError(tgot, e1, e)

    e match {
      case Print(e1) => typ(e1); TUndefined
      case N(_) => TNumber
      case B(_) => TBool
      case Undefined => TUndefined
      case S(_) => TString
      case Var(x) => env(x)
      case ConstDecl(x, e1, e2) => typeInfer(env + (x -> typ(e1)), e2)
      case Unary(Neg, e1) => typ(e1) match {
        case TNumber => TNumber
        case tgot => err(tgot, e1)
      }
      case Unary(Not, e1) => typ(e1) match {
        case TBool => TBool
        case tgot => err(tgot, e1)
      }
      case Binary(Plus, e1, e2) => typ(e1) match {
        case TNumber => typ(e2) match {
          case TNumber => TNumber
          case tgot => err(tgot, e2)
        }
        case TString => typ(e2) match {
          case TString => TString
          case tgot => err(tgot, e2)
        }
        case tgot => err(tgot, e1)
      }
        
      case Binary(Minus|Times|Div, e1, e2) => typ(e1) match {
        case TNumber => typ(e2) match {
          case TNumber => TNumber
          case tgot => err(tgot, e2)
        }
        case tgot => err(tgot, e1)
      }
        
      case Binary(Eq|Ne, e1, e2) => {
        if      (hasFunctionTyp(typ(e1))) err(typ(e1), e1)
        else if (hasFunctionTyp(typ(e2))) err(typ(e2), e2)
        else    TBool
      }

      case Binary(Lt|Le|Gt|Ge, e1, e2) => typ(e1) match {
        case TNumber => typ(e2) match {
          case TNumber => TBool
          case tgot => err(tgot, e2)
        }
        case TString => typ(e2) match {
          case TString => TBool
          case tgot => err(tgot, e2)
        }
        case tgot => err(tgot, e1)
      }
        
      case Binary(And|Or, e1, e2) => typ(e1) match {
        case TBool => typ(e2) match {
          case TBool => TBool
          case tgot => err(tgot, e2)
        }
        case tgot => err(tgot, e1)
      }
        
      case Binary(Seq, e1, e2) => typ(e1); typ(e2)
        
      case If(e1, e2, e3) => typ(e1) match {
        case TBool => {
          if (typ(e2) == typ(e3)) typ(e2)
          else err(typ(e3), e3)
        }
        case tgot => err(tgot, e1)
      }
        
      case Function(p, params, tann, e1) => {
        // Bind to env1 an environment that extends env with an appropriate binding if
        // the function is potentially recursive.
        val env1 = (p, tann) match {
          case (Some(f), Some(tret)) =>
            val tprime = TFunction(params, tret)
            env + (f -> tprime)
          case (None, _) => env
          case _ => err(TUndefined, e1)
        }
        // Bind to env2 an environment that extends env1 with bindings for params.
        val env2 = env1 ++ params
        // params.foldLeft (env1) { (x, y) => match y { case (varI, typI) => x + varI => typI}}
        // Match on whether the return type is specified // ADDED: and the inferred return type.
        (tann, typeInfer(env2, e1)) match {
          case (None, typR) => TFunction(params, typR)
          case (Some(tret), typR) => if (tret == typR) TFunction(params, typR) else err(typR, e1)
        }
      }
      

      // Call takes args from the caller and matches them against the params that the function called is defined with.
      // Eg, function: def func(a:TNumber, b:TNumber) has args: List(("a", TNumber), ("b", TNumber))
      // when you call this function, func(7, 9), then your params are similar to List(7, 9)
      //
      // This means the length of the number of args passed in should match the number of params in
      // the function definition and their types should match. 
      //
      // The only thing we have to check here is that the types of params match the types of args

      case Call(e1, args) => typ(e1) match {
        case TFunction(params, tret) if (params.length == args.length) => {
          (params, args).zipped.foreach {
            (a, b) => (a, typ(b)) match {
              case ( (str, typA), typB ) => if (typA != typB) err(typA, b)
            }
          };
          tret
        }
        case tgot => err(tgot, e1)
      }

      // Learn exactly whats going on here
      // maps the object to a standard jsy object type, with string fields mapped to their jsy tfields
      case Obj(fields) => TObj( fields.map( s => (s._1, typeInfer(env, fields(s._1)) ) ) )

      // Judgemnet form says e must be object 'e: {...., , ....}'
      case GetField(e1, f) => typ(e1) match {
        case TObj(tfields) => tfields(f)
        case tgot => err(tgot, e1)
      }
    }
  }
  
  
  /* Small-Step Interpreter */
  
  def inequalityVal(bop: Bop, v1: Expr, v2: Expr): Boolean = {
    require(bop == Lt || bop == Le || bop == Gt || bop == Ge)
    ((v1, v2): @unchecked) match {
      case (S(s1), S(s2)) =>
        (bop: @unchecked) match {
          case Lt => s1 < s2
          case Le => s1 <= s2
          case Gt => s1 > s2
          case Ge => s1 >= s2
        }
      case (N(n1), N(n2)) =>
        (bop: @unchecked) match {
          case Lt => n1 < n2
          case Le => n1 <= n2
          case Gt => n1 > n2
          case Ge => n1 >= n2
        }
    }
  }
  
  def substitute(e: Expr, v: Expr, x: String): Expr = {
    require(isValue(v))
    
    def subst(e: Expr): Expr = substitute(e, v, x)
    
    e match {
      case N(_) | B(_) | Undefined | S(_) => e
      case Print(e1) => Print(subst(e1))
      case Unary(uop, e1) => Unary(uop, subst(e1))
      case Binary(bop, e1, e2) => Binary(bop, subst(e1), subst(e2))
      case If(e1, e2, e3) => If(subst(e1), subst(e2), subst(e3))
      case Var(y) => if (x == y) v else e
      case ConstDecl(y, e1, e2) => ConstDecl(y, subst(e1), if (x == y) e2 else subst(e2))
      // Learn exactly whats going on here
      case Function(p, params, tann, e1) =>
        if (params.exists((t1: (String, Typ)) => t1._1 == x) || p == Some(x)) {
          Function(p, params, tann, e1)
        }
        else Function(p, params, tann, subst(e1))
      // have to substitue for each arg in the call e[vn/xn][vn-1/xn-1]
      case Call(e1, args) => Call(subst(e1), args map subst) //args.map (v1 => subst(v1)) )
      // Learn exactly whats going on here
      case Obj(fields) => Obj( fields.map( s => (s._1, subst(s._2) )))
      // Learn exactly whats going on here
      case GetField(e1, f) => GetField(subst(e1), f)
    }
  }
  
  def step(e: Expr): Expr = {
    require(!isValue(e))
    
    def stepIfNotValue(e: Expr): Option[Expr] = if (isValue(e)) None else Some(step(e))
    
    e match {
      /* Base Cases: Do Rules */
      case Print(v1) if isValue(v1) => println(pretty(v1)); Undefined
      case Unary(Neg, N(n1)) => N(- n1)
      case Unary(Not, B(b1)) => B(! b1)
      case Binary(Seq, v1, e2) if isValue(v1) => e2
      case Binary(Plus, S(s1), S(s2)) => S(s1 + s2)
      case Binary(Plus, N(n1), N(n2)) => N(n1 + n2)
      case Binary(bop @ (Lt|Le|Gt|Ge), v1, v2) if isValue(v1) && isValue(v2) => B(inequalityVal(bop, v1, v2))
      case Binary(Eq, v1, v2) if isValue(v1) && isValue(v2) => B(v1 == v2)
      case Binary(Ne, v1, v2) if isValue(v1) && isValue(v2) => B(v1 != v2)
      case Binary(And, B(b1), e2) => if (b1) e2 else B(false)
      case Binary(Or, B(b1), e2) => if (b1) B(true) else e2
      case ConstDecl(x, v1, e2) if isValue(v1) => substitute(e2, v1, x)
      case Call(v1, args) if isValue(v1) && (args forall isValue) =>
        v1 match {
          case Function(p, params, _, e1) => {
            val e1p = (params, args).zipped.foldRight(e1){
              // Learn exactly whats going on here
              (paramsX, acc) => paramsX match {
                case ((paramName, _ ), argValue) => substitute(acc, argValue, paramName)
              }
            }
            p match {
              case None => e1p
              case Some(x1) => substitute(e1p, v1, x1)
            }
          }
          case _ => throw new StuckError(e)
        }
      /*** Fill-in more cases here. ***/

      // Implement DoMinus, DoDiv, DoTime)
      case Binary(Minus, N(n1), N(n2)) => N(n1 - n2) 
      // Implement DoDiv 
      case Binary(Div, N(n1), N(n2)) => N(n1 / n2)
      // Implement DoTimes 
      case Binary(Times, N(n1), N(n2)) => N(n1 * n2)
      // Learn exactly whats going on here
      case GetField(Obj(fields), f) => fields.get(f) match {
        case Some(e1) => e1 
        case None => throw new StuckError(e)
      }
      case If(B(b1), e2, e3) => {
          if (b1) e2 else e3   
      }
      // // Learn exactly whats going on here
      
      /* Inductive Cases: Search Rules */
      case Print(e1) => Print(step(e1))
      case Unary(uop, e1) => Unary(uop, step(e1))
      case Binary(bop, v1, e2) if isValue(v1) => Binary(bop, v1, step(e2))
      case Binary(bop, e1, e2) => Binary(bop, step(e1), e2)
      case If(e1, e2, e3) => If(step(e1), e2, e3)
      case ConstDecl(x, e1, e2) => ConstDecl(x, step(e1), e2)
      /*** Fill-in more cases here. ***/

      // Learn exactly whats going on here
      case GetField(e1, f) => GetField(step(e1), f) 

      case Obj(fields) => Obj(fields.map{case (a,b) => (a, step(b))})
      // Learn exactly whats going on here
      case Call(v1,args) if isValue(v1)=> Call(v1, mapFirst(stepIfNotValue)(args))  
      case Call(e1,e2)=> Call(step(e1),e2)

      
      /* Everything else is a stuck error. Should not happen if e is well-typed. */
      case _ => throw StuckError(e)
    }
  }
  
  
  /* External Interfaces */
  
  this.debug = true // comment this out or set to false if you don't want print debugging information
  
  def inferType(e: Expr): Typ = {
    if (debug) {
      println("------------------------------------------------------------")
      println("Type checking: %s ...".format(e))
    } 
    val t = typeInfer(Map.empty, e)
    if (debug) {
      println("Type: " + pretty(t))
    }
    t
  }
  
  // Interface to run your small-step interpreter and print out the steps of evaluation if debugging. 
  def iterateStep(e: Expr): Expr = {
    require(closed(e))
    def loop(e: Expr, n: Int): Expr = {
      if (debug) { println("Step %s: %s".format(n, e)) }
      if (isValue(e)) e else loop(step(e), n + 1)
    }
    if (debug) {
      println("------------------------------------------------------------")
      println("Evaluating with step ...")
    }
    val v = loop(e, 0)
    if (debug) {
      println("Value: " + v)
    }
    v
  }

  // Convenience to pass in a jsy expression as a string.
  def iterateStep(s: String): Expr = iterateStep(Parser.parse(s))
  
  // Interface for main
  def processFile(file: java.io.File) {
    if (debug) {
      println("============================================================")
      println("File: " + file.getName)
      println("Parsing ...")
    }
    
    val expr =
      handle(None: Option[Expr]) {Some{
        Parser.parseFile(file)
      }} getOrElse {
        return
      }
    
    handle() {
      val t = inferType(expr)
    }
    
    handle() {
      val v1 = iterateStep(expr)
      println(pretty(v1))
    }
  }

}