package com.lordofthejars.asciidoctorfy;

import static com.lordofthejars.asciidoctorfy.IOUtils.copyInputStream;
import static com.lordofthejars.asciidoctorfy.IOUtils.join;
import static com.lordofthejars.asciidoctorfy.IOUtils.readFull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.antlrjavaparser.JavaParser;
import com.github.antlrjavaparser.ParseException;
import com.github.antlrjavaparser.api.BlockComment;
import com.github.antlrjavaparser.api.Comment;
import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.ImportDeclaration;
import com.github.antlrjavaparser.api.LineComment;
import com.github.antlrjavaparser.api.PackageDeclaration;
import com.github.antlrjavaparser.api.TypeParameter;
import com.github.antlrjavaparser.api.body.AnnotationDeclaration;
import com.github.antlrjavaparser.api.body.AnnotationMemberDeclaration;
import com.github.antlrjavaparser.api.body.BodyDeclaration;
import com.github.antlrjavaparser.api.body.CatchParameter;
import com.github.antlrjavaparser.api.body.ClassOrInterfaceDeclaration;
import com.github.antlrjavaparser.api.body.ConstructorDeclaration;
import com.github.antlrjavaparser.api.body.EmptyMemberDeclaration;
import com.github.antlrjavaparser.api.body.EmptyTypeDeclaration;
import com.github.antlrjavaparser.api.body.EnumConstantDeclaration;
import com.github.antlrjavaparser.api.body.EnumDeclaration;
import com.github.antlrjavaparser.api.body.FieldDeclaration;
import com.github.antlrjavaparser.api.body.InitializerDeclaration;
import com.github.antlrjavaparser.api.body.JavadocComment;
import com.github.antlrjavaparser.api.body.MethodDeclaration;
import com.github.antlrjavaparser.api.body.Parameter;
import com.github.antlrjavaparser.api.body.Resource;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import com.github.antlrjavaparser.api.body.VariableDeclarator;
import com.github.antlrjavaparser.api.body.VariableDeclaratorId;
import com.github.antlrjavaparser.api.expr.ArrayAccessExpr;
import com.github.antlrjavaparser.api.expr.ArrayCreationExpr;
import com.github.antlrjavaparser.api.expr.ArrayInitializerExpr;
import com.github.antlrjavaparser.api.expr.AssignExpr;
import com.github.antlrjavaparser.api.expr.BinaryExpr;
import com.github.antlrjavaparser.api.expr.BooleanLiteralExpr;
import com.github.antlrjavaparser.api.expr.CastExpr;
import com.github.antlrjavaparser.api.expr.CharLiteralExpr;
import com.github.antlrjavaparser.api.expr.ClassExpr;
import com.github.antlrjavaparser.api.expr.ConditionalExpr;
import com.github.antlrjavaparser.api.expr.DoubleLiteralExpr;
import com.github.antlrjavaparser.api.expr.EnclosedExpr;
import com.github.antlrjavaparser.api.expr.FieldAccessExpr;
import com.github.antlrjavaparser.api.expr.InstanceOfExpr;
import com.github.antlrjavaparser.api.expr.IntegerLiteralExpr;
import com.github.antlrjavaparser.api.expr.IntegerLiteralMinValueExpr;
import com.github.antlrjavaparser.api.expr.LongLiteralExpr;
import com.github.antlrjavaparser.api.expr.LongLiteralMinValueExpr;
import com.github.antlrjavaparser.api.expr.MarkerAnnotationExpr;
import com.github.antlrjavaparser.api.expr.MemberValuePair;
import com.github.antlrjavaparser.api.expr.MethodCallExpr;
import com.github.antlrjavaparser.api.expr.NameExpr;
import com.github.antlrjavaparser.api.expr.NormalAnnotationExpr;
import com.github.antlrjavaparser.api.expr.NullLiteralExpr;
import com.github.antlrjavaparser.api.expr.ObjectCreationExpr;
import com.github.antlrjavaparser.api.expr.QualifiedNameExpr;
import com.github.antlrjavaparser.api.expr.SingleMemberAnnotationExpr;
import com.github.antlrjavaparser.api.expr.StringLiteralExpr;
import com.github.antlrjavaparser.api.expr.SuperExpr;
import com.github.antlrjavaparser.api.expr.ThisExpr;
import com.github.antlrjavaparser.api.expr.UnaryExpr;
import com.github.antlrjavaparser.api.expr.VariableDeclarationExpr;
import com.github.antlrjavaparser.api.stmt.AssertStmt;
import com.github.antlrjavaparser.api.stmt.BlockStmt;
import com.github.antlrjavaparser.api.stmt.BreakStmt;
import com.github.antlrjavaparser.api.stmt.CatchClause;
import com.github.antlrjavaparser.api.stmt.ContinueStmt;
import com.github.antlrjavaparser.api.stmt.DoStmt;
import com.github.antlrjavaparser.api.stmt.EmptyStmt;
import com.github.antlrjavaparser.api.stmt.ExplicitConstructorInvocationStmt;
import com.github.antlrjavaparser.api.stmt.ExpressionStmt;
import com.github.antlrjavaparser.api.stmt.ForStmt;
import com.github.antlrjavaparser.api.stmt.ForeachStmt;
import com.github.antlrjavaparser.api.stmt.IfStmt;
import com.github.antlrjavaparser.api.stmt.LabeledStmt;
import com.github.antlrjavaparser.api.stmt.ReturnStmt;
import com.github.antlrjavaparser.api.stmt.SwitchEntryStmt;
import com.github.antlrjavaparser.api.stmt.SwitchStmt;
import com.github.antlrjavaparser.api.stmt.SynchronizedStmt;
import com.github.antlrjavaparser.api.stmt.ThrowStmt;
import com.github.antlrjavaparser.api.stmt.TryStmt;
import com.github.antlrjavaparser.api.stmt.TypeDeclarationStmt;
import com.github.antlrjavaparser.api.stmt.WhileStmt;
import com.github.antlrjavaparser.api.type.ClassOrInterfaceType;
import com.github.antlrjavaparser.api.type.PrimitiveType;
import com.github.antlrjavaparser.api.type.ReferenceType;
import com.github.antlrjavaparser.api.type.VoidType;
import com.github.antlrjavaparser.api.type.WildcardType;
import com.github.antlrjavaparser.api.visitor.VoidVisitor;

public class Java7Parser {

    private static final String CALLOUT_PATTERN_LINE = ".*//\\s+<\\d+>.*";
    private static final Pattern CALLOUT_NUMBER_PATTERN = Pattern.compile("<\\d+>");

    private StringBuilder output = new StringBuilder();
    private StringBuilder callouts = new StringBuilder();

    class SearchElement implements VoidVisitor<Map<String, Object>> {

        private StringBuilder source = new StringBuilder();

        private String getMethodSignature(MethodDeclaration n) {

            String pattern = n.getName() + "\\s*[(]\\s*";

            String params = "";
            if (n.getParameters() != null) {
                for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                    Parameter p = i.next();
                    params += p.getType() + "\\s*";
                    if (i.hasNext()) {
                        params += "," + "\\s*";
                    }
                }
            }

            pattern += params;

            pattern += "\\s*[)]";

            return pattern;
        }

        @Override
        public void visit(CompilationUnit n, Map<String, Object> arg) {

        }

        @Override
        public void visit(PackageDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ImportDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(TypeParameter n, Map<String, Object> arg) {
        }

        @Override
        public void visit(LineComment n, Map<String, Object> arg) {
        }

        @Override
        public void visit(BlockComment n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(Comment n, Map<String, Object> arg) {
        }

        @Override
        public void visit(EnumDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(EmptyTypeDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(EnumConstantDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(AnnotationDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(AnnotationMemberDeclaration n, Map<String, Object> arg) {

        }

        @Override
        public void visit(FieldDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(VariableDeclarator n, Map<String, Object> arg) {

        }

        @Override
        public void visit(VariableDeclaratorId n, Map<String, Object> arg) {

        }

        @Override
        public void visit(ConstructorDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(MethodDeclaration n, Map<String, Object> arg) {

            if (arg.containsKey("method")) {
                String methodName = (String) arg.get("method");

                String patternMethod = getMethodSignature(n);
                if (methodName.matches(patternMethod)) {
                    source.append(getCodeFromOriginalFile(n, n.getBeginLine() - 1, n.getEndLine()));
                }
            }

        }

        @Override
        public void visit(Parameter n, Map<String, Object> arg) {
        }

        @Override
        public void visit(CatchParameter n, Map<String, Object> arg) {
        }

        @Override
        public void visit(Resource n, Map<String, Object> arg) {
        }

        @Override
        public void visit(EmptyMemberDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(InitializerDeclaration n, Map<String, Object> arg) {
        }

        @Override
        public void visit(JavadocComment n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ClassOrInterfaceType n, Map<String, Object> arg) {
        }

        @Override
        public void visit(PrimitiveType n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ReferenceType n, Map<String, Object> arg) {
        }

        @Override
        public void visit(VoidType n, Map<String, Object> arg) {
        }

        @Override
        public void visit(WildcardType n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ArrayAccessExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ArrayCreationExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ArrayInitializerExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(AssignExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(BinaryExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(CastExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ClassExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ConditionalExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(EnclosedExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(FieldAccessExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(InstanceOfExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(StringLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(IntegerLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(LongLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(IntegerLiteralMinValueExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(LongLiteralMinValueExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(CharLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(DoubleLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(BooleanLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(NullLiteralExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(MethodCallExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(NameExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ObjectCreationExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(QualifiedNameExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ThisExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(SuperExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(UnaryExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(VariableDeclarationExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(MarkerAnnotationExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(SingleMemberAnnotationExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(NormalAnnotationExpr n, Map<String, Object> arg) {
        }

        @Override
        public void visit(MemberValuePair n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ExplicitConstructorInvocationStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(TypeDeclarationStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(AssertStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(BlockStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(LabeledStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(EmptyStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ExpressionStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(SwitchStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(SwitchEntryStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(BreakStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ReturnStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(IfStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(WhileStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ContinueStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(DoStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ForeachStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ForStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(ThrowStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(SynchronizedStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(TryStmt n, Map<String, Object> arg) {
        }

        @Override
        public void visit(CatchClause n, Map<String, Object> arg) {
        }

    }

    private String[] originalSourceCode = new String[0];
    private ByteArrayInputStream originalStream = null;

    public ContentAndCallouts extract(InputStream is, Map<String, Object> attributes) throws ParseException,
            IOException {

        this.originalStream = copyInputStream(is);
        CompilationUnit compilationUnit = JavaParser.parse(originalStream);

        this.originalStream.reset();
        this.originalSourceCode = readFull(this.originalStream);

        boolean clazz = attributes.containsKey("class");

        final List<TypeDeclaration> types = compilationUnit.getTypes();

        for (TypeDeclaration typeDeclaration : types) {

            if (clazz) {
                final String bodyClass = getCodeFromOriginalFile(typeDeclaration, typeDeclaration.getBeginLine() - 1,
                        typeDeclaration.getEndLine());
                output.append(bodyClass);
            } else {

                final List<BodyDeclaration> members = typeDeclaration.getMembers();
                for (BodyDeclaration bodyDeclaration : members) {
                    final SearchElement v = new SearchElement();
                    bodyDeclaration.accept(v, attributes);
                    output.append(v.source.toString());
                }
            }

        }

        return new ContentAndCallouts(this.output.toString(), this.callouts.toString());

    }

    private String getCodeFromOriginalFile(BodyDeclaration n, int beginLine, int endLine) {

        final String[] fieldsArray = Arrays.copyOfRange(originalSourceCode, n.getBeginLine() - 1, n.getEndLine());

        String content = renderContent(extractCallouts(fieldsArray));
        return content;
    }

    private String[] extractCallouts(String[] lines) {

        String[] contentWithoutCallouts = new String[lines.length];

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.matches(CALLOUT_PATTERN_LINE)) {

                Matcher matcher = CALLOUT_NUMBER_PATTERN.matcher(line);

                if (matcher.find()) {

                    int startCalloutNumber = matcher.start();
                    int endCalloutNumber = matcher.end();

                    contentWithoutCallouts[i] = line.substring(0, endCalloutNumber);
                    String calloutNumber = line.substring(startCalloutNumber, endCalloutNumber);

                    this.callouts.append(calloutNumber + " " + line.substring(endCalloutNumber).trim()).append(
                            IOUtils.NEW_LINE);
                } else {
                    contentWithoutCallouts[i] = lines[i];
                }
            } else {
                contentWithoutCallouts[i] = lines[i];
            }
        }

        return contentWithoutCallouts;

    }

    private String renderContent(String[] content) {

        String output = "";

        if (content.length > 0) {
            final int initialLength = content[0].length();
            int shiftedToInitial = content[0].trim().length();

            int extraChars = initialLength - shiftedToInitial;

            return join(content, extraChars);

        }

        return output;

    }

}