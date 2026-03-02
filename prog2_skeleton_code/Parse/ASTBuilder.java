package Parse;

import java.util.ArrayList;
import Absyn.*;
import java.util.Optional;
import Parse.antlr_build.Parse.*;
import org.antlr.v4.runtime.ParserRuleContext;

public class ASTBuilder extends gParserBaseVisitor<Absyn> {

   @Override
   public Absyn visitProgram(gParser.ProgramContext ctx) {
      DeclList decls = new DeclList(0);
      for (gParser.DeclarationContext dctx : ctx.declaration()) {
        decls.list.add((Decl) visit(dctx));
      }
      return decls;
   }

   @Override
   public Absyn visitCompStmt(gParser.CompStmtContext ctx) {
      int pos = ctx.getStart().getLine();
      DeclList decls = new DeclList(pos);
      StmtList stmts = new StmtList(pos);
      for (gParser.DeclarationContext dctx : ctx.declaration()) {
         decls.list.add((Decl) visit(dctx));
      }
      for (gParser.StatementContext sctx : ctx.statement()) {
         stmts.list.add((Stmt) visit(sctx));
      }
      return new CompStmt(pos, decls, stmts);
   }

   @Override
   public Absyn visitIfStmt(gParser.IfStmtContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp cond = (Exp) visit(ctx.expr());
      Stmt body = (Stmt) visit(ctx.statement());
      return new IfStmt(pos, cond, body, new EmptyStmt(pos));
   }

   @Override
   public Absyn visitIfElseStmt(gParser.IfElseStmtContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp cond = (Exp) visit(ctx.expr());
      Stmt ifBody  = (Stmt) visit(ctx.statement(0));
      Stmt elseBody = (Stmt) visit(ctx.statement(1));
      return new IfStmt(pos, cond, ifBody, elseBody);
   }

   @Override
   public Absyn visitWhileStmt(gParser.WhileStmtContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp cond = (Exp) visit(ctx.expr());
      Stmt body = (Stmt) visit(ctx.statement());
      return new WhileStmt(pos, cond, body);
   }

   @Override
   public Absyn visitExprStmt(gParser.ExprStmtContext ctx) {
      int pos = ctx.getStart().getLine();
      return new ExprStmt(pos, (Exp) visit(ctx.expr()));
   }

   @Override
   public Absyn visitReturnStmt(gParser.ReturnStmtContext ctx) {
      int pos = ctx.getStart().getLine();
      return new ReturnStmt(pos, (Exp) visit(ctx.initializer()));
   }

   @Override
   public Absyn visitBreakStmt(gParser.BreakStmtContext ctx) {
      return new BreakStmt(ctx.getStart().getLine());
   }

   @Override
   public Absyn visitVarDecl(gParser.VarDeclContext ctx) {
      int pos = ctx.getStart().getLine();
      Type t = (Type) visit(ctx.type());
      String name = ctx.ID().getText();
      Exp init = (Exp) visit(ctx.initialization());
      return new VarDecl(pos, t, name, init);
   }

   @Override
   public Absyn visitFunDecl(gParser.FunDeclContext ctx) {
      int pos = ctx.getStart().getLine();
      Type t = (Type) visit(ctx.type());
      String name = ctx.ID().getText();
      DeclList params = new DeclList(pos);
      if (ctx.parameters() != null) {
         params = (DeclList) visit(ctx.parameters());
      }
      Stmt body = (Stmt) visit(ctx.statement());
      return new FunDecl(pos, t, name, params, body);
   }

   @Override
   public Absyn visitTypedefDecl(gParser.TypedefDeclContext ctx) {
      int pos = ctx.getStart().getLine();
      Type t = (Type) visit(ctx.type());
      String name = ctx.ID().getText();
      return new Typedef(pos, t, name);
   }

   @Override
   public Absyn visitStructOrUnionDecl(gParser.StructOrUnionDeclContext ctx) {
      int pos = ctx.getStart().getLine();
      boolean isStruct = ctx.STRUCT() != null;
      String name = ctx.ID(0).getText();
      DeclList body = new DeclList(pos);
      java.util.List<gParser.TypeContext> types = ctx.type();
      for (int i = 0; i < types.size(); i++) {
         Type t = (Type) visit(types.get(i));
         String memberName = ctx.ID(i + 1).getText();
         if (isStruct) {
            body.list.add(new StructMember(pos, t, memberName));
         } else {
            body.list.add(new UnionMember(pos, t, memberName));
         }
      }
      if (isStruct) {
         return new StructDecl(pos, name, body);
      } else {
         return new UnionDecl(pos, name, body);
      }
   }

   @Override
   public Absyn visitType(gParser.TypeContext ctx) {
      int pos = ctx.getStart().getLine();
      boolean isConst = ctx.CONST() != null;
      String typeName = ctx.type_name().getText();
      int stars = ctx.STAR().size();
      DeclList brackets = new DeclList(pos);
      if (ctx.brackets_list() != null) {
         brackets = (DeclList) visit(ctx.brackets_list());
      }
      return new Type(pos, isConst, typeName, stars, brackets);
   }

   @Override
   public Absyn visitEmptyArrayBrackets(gParser.EmptyArrayBracketsContext ctx) {
      int pos = ctx.getStart().getLine();
      int count = ctx.LSQUARE().size();
      DeclList brackets = new DeclList(pos);
      for (int i = 0; i < count; i++) {
         brackets.list.add(new ArrayType(pos, new EmptyExp(pos)));
      }
      return brackets;
   }

   @Override
   public Absyn visitExprArrayBrackets(gParser.ExprArrayBracketsContext ctx) {
      int pos = ctx.getStart().getLine();
      DeclList brackets = new DeclList(pos);
      for (gParser.ExprContext ectx : ctx.expr()) {
         brackets.list.add(new ArrayType(pos, (Exp) visit(ectx)));
      }
      return brackets;
   }

   @Override
   public Absyn visitInitialization(gParser.InitializationContext ctx) {
      int pos = ctx.getStart().getLine();
      if (ctx.initializer() == null) {
         return new EmptyExp(pos);
      }
      return visit(ctx.initializer());
   }

   @Override
   public Absyn visitParameters(gParser.ParametersContext ctx) {
      int pos = ctx.getStart().getLine();
      DeclList params = new DeclList(pos);
      java.util.List<gParser.TypeContext> types = ctx.type();
      for (int i = 0; i < types.size(); i++) {
         Type t = (Type) visit(types.get(i));
         String name = ctx.ID(i).getText();
         params.list.add(new Parameter(pos, t, name));
      }
      return params;
   }

   @Override
   public Absyn visitInitializer(gParser.InitializerContext ctx) {
      if (ctx.expr() != null) {
         return visit(ctx.expr());
      }
      int pos = ctx.getStart().getLine();
      ExpList list = new ExpList(pos);
      for (gParser.InitializerContext ictx : ctx.initializer()) {
         list.list.add((Exp) visit(ictx));
      }
      return list;
   }

   @Override
   public Absyn visitParenExp(gParser.ParenExpContext ctx) {
      return visit(ctx.expr());
   }

   @Override
   public Absyn visitBinOp(gParser.BinOpContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp left = (Exp) visit(ctx.expr(0));
      Exp right = (Exp) visit(ctx.expr(1));
      String op = ctx.op.getText();
      return new BinOp(pos, left, op, right);
   }

   @Override
   public Absyn visitFunExp(gParser.FunExpContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp name = (Exp) visit(ctx.expr(0));
      ExpList args = new ExpList(pos);
      java.util.List<gParser.ExprContext> exprs = ctx.expr();
      for (int i = 1; i < exprs.size(); i++) {
         args.list.add((Exp) visit(exprs.get(i)));
      }
      return new FunExp(pos, name, args);
   }

   @Override
   public Absyn visitArrayExp(gParser.ArrayExpContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp name = (Exp) visit(ctx.expr(0));
      ExpList indices = new ExpList(pos);
      java.util.List<gParser.ExprContext> exprs = ctx.expr();
      for (int i = 1; i < exprs.size(); i++) {
         indices.list.add((Exp) visit(exprs.get(i)));
      }
      return new ArrayExp(pos, name, indices);
   }

   @Override
   public Absyn visitUnaryExp(gParser.UnaryExpContext ctx) {
      int pos = ctx.getStart().getLine();
      String op = ctx.unary_operator().getText();
      Exp e = (Exp) visit(ctx.expr());
      return new UnaryExp(pos, op, e);
   }

   @Override
   public Absyn visitAssignExp(gParser.AssignExpContext ctx) {
      int pos = ctx.getStart().getLine();
      Exp left = (Exp) visit(ctx.expr());
      Exp right = (Exp) visit(ctx.initializer());
      return new AssignExp(pos, left, right);
   }

   @Override
   public Absyn visitDecLit(gParser.DecLitContext ctx) {
      int pos = ctx.getStart().getLine();
      int val = Integer.decode(ctx.DECIMAL_LITERAL().getText());
      return new DecLit(pos, val);
   }

   @Override
   public Absyn visitID(gParser.IDContext ctx) {
      int pos = ctx.getStart().getLine();
      return new ID(pos, ctx.ID().getText());
   }

   @Override
   public Absyn visitStrLit(gParser.StrLitContext ctx) {
      int pos = ctx.getStart().getLine();
      return new StrLit(pos, ctx.STRING_LITERAL().getText());
   }

}
