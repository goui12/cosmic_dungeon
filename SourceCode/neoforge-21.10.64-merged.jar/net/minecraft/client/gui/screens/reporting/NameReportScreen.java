package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
    private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
    private static final Component COMMENT_BOX_LABEL = Component.translatable("gui.abuseReport.name.comment_box_label");
    @Nullable
    private MultiLineEditBox commentBox;

    private NameReportScreen(Screen lastScreen, ReportingContext reportingContext, NameReport.Builder reportBuilder) {
        super(TITLE, lastScreen, reportingContext, reportBuilder);
    }

    public NameReportScreen(Screen lastScreen, ReportingContext reportingContext, UUID reportedProfileId, String reportedName) {
        this(lastScreen, reportingContext, new NameReport.Builder(reportedProfileId, reportedName, reportingContext.sender().reportLimits()));
    }

    public NameReportScreen(Screen lastScreen, ReportingContext reportingContext, NameReport report) {
        this(lastScreen, reportingContext, new NameReport.Builder(report, reportingContext.sender().reportLimits()));
    }

    @Override
    protected void addContent() {
        Component component = Component.literal(this.reportBuilder.report().getReportedName()).withStyle(ChatFormatting.YELLOW);
        this.layout
            .addChild(
                new StringWidget(Component.translatable("gui.abuseReport.name.reporting", component), this.font),
                p_359100_ -> p_359100_.alignHorizontallyCenter().padding(0, 8)
            );
        this.commentBox = this.createCommentBox(280, 9 * 8, p_436489_ -> {
            this.reportBuilder.setComments(p_436489_);
            this.onReportChanged();
        });
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, COMMENT_BOX_LABEL, p_299902_ -> p_299902_.paddingBottom(12)));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (super.mouseReleased(event)) {
            return true;
        } else {
            return this.commentBox != null ? this.commentBox.mouseReleased(event) : false;
        }
    }
}
